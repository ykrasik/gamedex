/****************************************************************************
 * Copyright (C) 2016-2019 Yevgeny Krasik                                   *
 *                                                                          *
 * Licensed under the Apache License, Version 2.0 (the "License");          *
 * you may not use this file except in compliance with the License.         *
 * You may obtain a copy of the License at                                  *
 *                                                                          *
 * http://www.apache.org/licenses/LICENSE-2.0                               *
 *                                                                          *
 * Unless required by applicable law or agreed to in writing, software      *
 * distributed under the License is distributed on an "AS IS" BASIS,        *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. *
 * See the License for the specific language governing permissions and      *
 * limitations under the License.                                           *
 ****************************************************************************/

package com.gitlab.ykrasik.gamedex.provider.igdb

import com.gitlab.ykrasik.gamedex.GameData
import com.gitlab.ykrasik.gamedex.ImageUrls
import com.gitlab.ykrasik.gamedex.Platform
import com.gitlab.ykrasik.gamedex.Score
import com.gitlab.ykrasik.gamedex.provider.*
import com.gitlab.ykrasik.gamedex.util.getResourceAsByteArray
import com.gitlab.ykrasik.gamedex.util.logResult
import com.gitlab.ykrasik.gamedex.util.logger
import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat
import org.slf4j.Logger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 29/01/2017
 * Time: 11:14
 */
@Singleton
class IgdbProvider @Inject constructor(private val config: IgdbConfig, private val client: IgdbClient) : GameProvider {
    private val log = logger()

    override suspend fun search(query: String, platform: Platform, account: ProviderUserAccount?): List<ProviderSearchResult> {
        val results = log.logResult("[$platform] Searching '$query'...", { results -> "${results.size} results." }, Logger::debug) {
            client.search(query, platform, account as IgdbUserAccount).toProviderSearchResults(query, platform)
        }
        results.forEach { log.trace(it.toString()) }
        return results
    }

    private fun List<IgdbClient.SearchResult>.toProviderSearchResults(name: String, platform: Platform): List<ProviderSearchResult> {
        // IGBD search sucks. It returns way more results then it should.
        // Since I couldn't figure out how to make it not return irrelevant results, I had to filter results myself.
        val searchWords = name.split("[^a-zA-Z\\d']".toRegex())
        val filteredResults = this.asSequence().filter { (_, name) ->
            searchWords.all { word ->
                name.contains(word, ignoreCase = true)
            }
        }
        return filteredResults.map { it.toSearchResult(platform) }.toList()
    }

    private fun IgdbClient.SearchResult.toSearchResult(platform: Platform) = ProviderSearchResult(
        apiUrl = "${config.baseUrl}/$id",
        name = name,
        description = summary,
        releaseDate = releaseDates?.findReleaseDate(platform),
        criticScore = toScore(aggregatedRating, aggregatedRatingCount),
        userScore = toScore(rating, ratingCount),
        thumbnailUrl = cover?.cloudinaryId?.toImageUrl(config.thumbnailImageType)
    )

    override suspend fun download(apiUrl: String, platform: Platform, account: ProviderUserAccount?): ProviderDownloadData =
        log.logResult("[$platform] Downloading $apiUrl...", log = Logger::debug) {
            client.fetch(apiUrl, account as IgdbUserAccount).toProviderData(platform)
        }

    private fun IgdbClient.DetailsResult.toProviderData(platform: Platform) = ProviderDownloadData(
        gameData = GameData(
            name = this.name,
            description = this.summary,
            releaseDate = this.releaseDates?.findReleaseDate(platform),
            criticScore = toScore(aggregatedRating, aggregatedRatingCount),
            userScore = toScore(rating, ratingCount),
            genres = this.genres?.map { it.genreName } ?: emptyList(),
            imageUrls = ImageUrls(
                thumbnailUrl = this.cover?.cloudinaryId?.toThumbnailUrl(),
                posterUrl = this.cover?.cloudinaryId?.toPosterUrl(),
                screenshotUrls = this.screenshots?.mapNotNull { it.cloudinaryId?.toScreenshotUrl() } ?: emptyList()
            )
        ),
        siteUrl = this.url
    )

    private fun toScore(score: Double?, numReviews: Int?): Score? = score?.let { Score(it, numReviews!!) }

    private fun List<IgdbClient.ReleaseDate>.findReleaseDate(platform: Platform): String? {
        // IGDB returns all release dates for all platforms, not just the one we searched for.
        val releaseDate = this.find { it.platform == platform.platformId } ?: return null
        return try {
            LocalDate.parse(releaseDate.human, DateTimeFormat.forPattern("YYYY-MMM-dd")).toString()
        } catch (e: Exception) {
            releaseDate.human
        }
    }

    private fun String.toThumbnailUrl() = toImageUrl(config.thumbnailImageType)
    private fun String.toPosterUrl() = toImageUrl(config.posterImageType)
    private fun String.toScreenshotUrl() = toImageUrl(config.screenshotImageType)
    private fun String.toImageUrl(type: IgdbImageType) = "${config.baseImageUrl}/t_$type/$this.jpg"

    enum class IgdbImageType {
        micro, // 35 x 35
        micro_2x, // 70 x 70
        thumb, // 90 x 90
        thumb_2x, // 180 x 180
        logo_med, // 284 x 160
        logo_med_2x, // 568 x 320
        cover_small, // 90 x 128
        cover_small_2x, // 180 x 256
        cover_big, // 227 x 320
        cover_big_2x, // 454 x 640
        screenshot_med, // 569 x 320
        screenshot_med_2x, // 1138 x 640
        screenshot_big, // 889 x 500
        screenshot_big_2x, // 1778 x 1000
        screenshot_huge, // 1280 x 720
        screenshot_huge_2x  // 2560 x 1440
    }

    private val Platform.platformId: Int get() = config.getPlatformId(this)
    private val Int.genreName: String get() = config.getGenreName(this)

    override val metadata = GameProviderMetadata(
        id = "Igdb",
        logo = getResourceAsByteArray("igdb.png"),
        supportedPlatforms = Platform.values().toList(),
        defaultOrder = config.defaultOrder,
        accountFeature = object : ProviderUserAccountFeature {
            override val accountUrl = config.accountUrl
            override val field1 = "Api Key"
            override fun createAccount(fields: Map<String, String>) = IgdbUserAccount(
                apiKey = fields.getValue(field1)
            )
        }
    )

    override fun toString() = id
}