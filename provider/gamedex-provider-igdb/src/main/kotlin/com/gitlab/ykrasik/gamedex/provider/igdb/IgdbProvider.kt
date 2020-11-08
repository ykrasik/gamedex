/****************************************************************************
 * Copyright (C) 2016-2020 Yevgeny Krasik                                   *
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
import com.gitlab.ykrasik.gamedex.Platform
import com.gitlab.ykrasik.gamedex.Score
import com.gitlab.ykrasik.gamedex.provider.GameProvider
import com.gitlab.ykrasik.gamedex.util.JodaLocalDate
import com.gitlab.ykrasik.gamedex.util.getResourceAsByteArray
import com.gitlab.ykrasik.gamedex.util.logResult
import com.gitlab.ykrasik.gamedex.util.logger
import org.slf4j.Logger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 29/01/2017
 * Time: 11:14
 */
@Singleton
class IgdbProvider @Inject constructor(
    private val config: IgdbConfig,
    private val client: IgdbClient,
) : GameProvider {
    private val log = logger()

    override suspend fun search(
        query: String,
        platform: Platform,
        account: GameProvider.Account,
        offset: Int,
        limit: Int,
    ): GameProvider.SearchResponse {
        val results = log.logResult("[$platform] Searching '$query'...", { results -> "${results.size} results." }, Logger::debug) {
            client.search(query, platform, account as IgdbUserAccount, offset = offset, limit = limit)
                .map { it.toSearchResult(platform) }
        }
        results.forEach { log.trace(it.toString()) }
        return GameProvider.SearchResponse(results, canShowMoreResults = null)
    }

    private fun IgdbClient.SearchResult.toSearchResult(platform: Platform) = GameProvider.SearchResult(
        providerGameId = id.toString(),
        name = name,
        description = summary,
        releaseDate = parseReleaseDate(platform),
        criticScore = parseCriticScore(),
        userScore = parseUserScore(),
        thumbnailUrl = cover?.imageId?.toImageUrl(config.thumbnailImageType)
    )

    override suspend fun fetch(providerGameId: String, platform: Platform, account: GameProvider.Account): GameProvider.FetchResponse =
        log.logResult("[$platform] Fetching IGDB game '$providerGameId'...", log = Logger::debug) {
            checkNotNull(client.fetch(providerGameId, account as IgdbUserAccount)?.toProviderData(platform)) {
                "Not found: IGDB game '$providerGameId'!"
            }
        }

    private fun IgdbClient.FetchResult.toProviderData(platform: Platform) = GameProvider.FetchResponse(
        gameData = GameData(
            name = this.name,
            description = this.summary,
            releaseDate = parseReleaseDate(platform),
            criticScore = parseCriticScore(),
            userScore = parseUserScore(),
            genres = this.genres?.map { it.genreName } ?: emptyList(),
            thumbnailUrl = this.cover?.imageId?.toThumbnailUrl(),
            posterUrl = this.cover?.imageId?.toPosterUrl(),
            screenshotUrls = this.screenshots?.mapNotNull { it.imageId?.toScreenshotUrl() } ?: emptyList()
        ),
        siteUrl = this.url
    )

    private fun IgdbClient.SharedSearchFetchFields.parseCriticScore(): Score? = toScore(aggregatedRating, aggregatedRatingCount)
    private fun IgdbClient.SharedSearchFetchFields.parseUserScore(): Score? = toScore(rating, ratingCount)
    private fun toScore(score: Double?, numReviews: Int?): Score? =
        if (score != null && numReviews != null && numReviews > 0) Score(score, numReviews) else null

    private fun IgdbClient.SharedSearchFetchFields.parseReleaseDate(platform: Platform): String? {
        // IGDB returns all release dates for all platforms, not just the one we searched for.
        val dateFromReleaseDates = releaseDates?.find { it.platform == platform.platformId }?.date?.takeIf { it != 0L }
        val dateWithFallback = dateFromReleaseDates ?: firstReleaseDate?.takeIf { it != 0L } ?: return null
        return JodaLocalDate(dateWithFallback * 1000).toString("YYYY-MM-dd")
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

    override val metadata = GameProvider.Metadata(
        id = id,
        logo = getResourceAsByteArray("igdb.png"),
        supportedPlatforms = supportedPlatforms,
        defaultOrder = config.defaultOrder,
        accountFeature = object : GameProvider.AccountFeature {
            override val accountUrl = config.accountUrl
            override val field1 = "Client ID"
            override val field2 = "Client Secret"
            override fun createAccount(fields: Map<String, String>) = IgdbUserAccount(
                clientId = fields.getValue(field1),
                clientSecret = fields.getValue(field2)
            )
        }
    )

    override fun toString() = id

    companion object {
        const val id = "Igdb"
        val supportedPlatforms = Platform.values().toList()
    }
}