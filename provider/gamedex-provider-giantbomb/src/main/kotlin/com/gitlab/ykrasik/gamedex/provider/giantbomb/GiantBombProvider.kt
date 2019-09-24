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

package com.gitlab.ykrasik.gamedex.provider.giantbomb

import com.gitlab.ykrasik.gamedex.GameData
import com.gitlab.ykrasik.gamedex.Platform
import com.gitlab.ykrasik.gamedex.provider.*
import com.gitlab.ykrasik.gamedex.util.getResourceAsByteArray
import com.gitlab.ykrasik.gamedex.util.logResult
import com.gitlab.ykrasik.gamedex.util.logger
import org.joda.time.LocalDate
import org.slf4j.Logger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 29/05/2016
 * Time: 10:53
 */
@Singleton
class GiantBombProvider @Inject constructor(
    private val config: GiantBombConfig,
    private val client: GiantBombClient
) : GameProvider {
    private val log = logger()

    override suspend fun search(
        query: String,
        platform: Platform,
        account: ProviderUserAccount,
        offset: Int,
        limit: Int
    ): List<ProviderSearchResult> {
        val results = log.logResult("[$platform] Searching '$query'...", { results -> "${results.size} results." }, Logger::debug) {
            val response = client.search(query, platform, account as GiantBombUserAccount, offset = offset, limit = limit)
            assertOk(response.statusCode)
            response.results.map { it.toProviderSearchResult() }
        }
        results.forEach { log.trace(it.toString()) }
        return results
    }

    private fun GiantBombClient.SearchResult.toProviderSearchResult() = ProviderSearchResult(
        providerGameId = apiDetailUrl,
        name = name,
        description = deck,
        releaseDate = parseReleaseDate(),
        criticScore = null,
        userScore = null,
        thumbnailUrl = image?.thumbUrl?.filterEmptyImage()
    )

    override suspend fun fetch(providerGameId: String, platform: Platform, account: ProviderUserAccount): ProviderFetchData =
        log.logResult("[$platform] Fetching GiantBomb url $providerGameId...", log = Logger::debug) {
            // providerGameId is the actual api url
            val response = client.fetch(providerGameId, account as GiantBombUserAccount)
            assertOk(response.statusCode)
            // When result is found - GiantBomb returns a Json object.
            // When result is not found, GiantBomb returns an empty Json array [].
            // So 'results' can contain at most a single value.
            response.results.first().toProviderData()
        }

    private fun GiantBombClient.DetailsResult.toProviderData() = ProviderFetchData(
        gameData = GameData(
            name = name,
            description = deck,
            releaseDate = parseReleaseDate(),
            criticScore = null,
            userScore = null,
            genres = genres?.map { it.name } ?: emptyList(),
            thumbnailUrl = image?.thumbUrl?.filterEmptyImage(),
            posterUrl = image?.superUrl?.filterEmptyImage(),
            screenshotUrls = images.mapNotNull { it.superUrl.filterEmptyImage() }
        ),
        siteUrl = siteDetailUrl
    )

    private fun String.filterEmptyImage(): String? = takeUnless { config.noImageFileNames.any { this.endsWith(it) } }

    private fun GiantBombClient.HasReleaseDate.parseReleaseDate(): String? {
        originalReleaseDate?.let { return it.toString() }

        val expectedReleaseYear = this.expectedReleaseYear ?: return null
        if (expectedReleaseQuarter != null) return "$expectedReleaseYear Q$expectedReleaseQuarter"

        var date = LocalDate(0).withYear(expectedReleaseYear)
        expectedReleaseMonth?.let { date = date.withMonthOfYear(it) }
        expectedReleaseDay?.let { date = date.withDayOfMonth(it) }
        return date.toString()
    }

    private fun assertOk(status: GiantBombClient.Status) {
        if (status != GiantBombClient.Status.OK) {
            throw IllegalStateException("Invalid statusCode: $status")
        }
    }

    override val metadata = GameProviderMetadata(
        id = "GiantBomb",
        logo = getResourceAsByteArray("giantbomb.png"),
        supportedPlatforms = Platform.values().toList(),
        defaultOrder = config.defaultOrder,
        accountFeature = object : ProviderUserAccountFeature {
            override val accountUrl = config.accountUrl
            override val field1 = "Api Key"
            override fun createAccount(fields: Map<String, String>) = GiantBombUserAccount(
                apiKey = fields.getValue(field1)
            )
        }
    )

    override fun toString() = id
}