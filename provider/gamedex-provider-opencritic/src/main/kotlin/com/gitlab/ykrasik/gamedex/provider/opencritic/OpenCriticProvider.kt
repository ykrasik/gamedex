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

package com.gitlab.ykrasik.gamedex.provider.opencritic

import com.gitlab.ykrasik.gamedex.GameData
import com.gitlab.ykrasik.gamedex.Platform
import com.gitlab.ykrasik.gamedex.Score
import com.gitlab.ykrasik.gamedex.provider.GameProvider
import com.gitlab.ykrasik.gamedex.util.getResourceAsByteArray
import com.gitlab.ykrasik.gamedex.util.logResult
import com.gitlab.ykrasik.gamedex.util.logger
import org.slf4j.Logger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 28/09/2019
 * Time: 22:12
 */
@Singleton
class OpenCriticProvider @Inject constructor(
    private val config: OpenCriticConfig,
    private val client: OpenCriticClient,
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
            client.search(query).mapIndexed { i, result -> result.toSearchResult(platform, account, i) }
        }
        results.forEach { log.trace(it.toString()) }

        // OpenCritic does not support pagination in search
        return GameProvider.SearchResponse(results, canShowMoreResults = false)
    }

    private suspend fun OpenCriticClient.SearchResult.toSearchResult(
        platform: Platform,
        account: GameProvider.Account,
        index: Int
    ): GameProvider.SearchResult {
        if (index > 0) {
            return GameProvider.SearchResult(
                providerGameId = id.toString(),
                name = name,
                description = null,
                releaseDate = null,
                criticScore = null,
                userScore = null,
                thumbnailUrl = null
            )
        }

        // Pre-fetch the first result, usually it's the correct one.
        val data = fetch(id.toString(), platform, account).gameData
        return GameProvider.SearchResult(
            providerGameId = id.toString(),
            name = data.name,
            description = data.description,
            releaseDate = data.releaseDate,
            criticScore = data.criticScore,
            userScore = data.userScore,
            thumbnailUrl = data.thumbnailUrl
        )
    }

    override suspend fun fetch(providerGameId: String, platform: Platform, account: GameProvider.Account): GameProvider.FetchResponse =
        log.logResult("[$platform] Fetching OpenCritic game '$providerGameId'...", log = Logger::debug) {
            client.fetch(providerGameId).toProviderData(platform)
        }

    private fun OpenCriticClient.FetchResult.toProviderData(platform: Platform) = GameProvider.FetchResponse(
        gameData = GameData(
            name = this.name,
            description = this.description?.takeUnless { it.isBlank() },
            releaseDate = this.Platforms.findReleaseDate(platform) ?: firstReleaseDate?.toLocalDate()?.toString(),
            criticScore = if (medianScore > 0 && numReviews != 0) Score(medianScore, numReviews) else null,
            userScore = null,
            genres = this.Genres.map { it.name },
            thumbnailUrl = this.verticalLogoScreenshot?.fullRes?.toImageUrl()
                ?: this.bannerScreenshot?.fullRes?.toImageUrl()
                ?: this.logoScreenshot?.thumbnail?.toImageUrl(),
            posterUrl = null,
            screenshotUrls = (this.mastheadScreenshot?.let { listOf(it.fullRes.toImageUrl()) }
                ?: emptyList()) + this.screenshots.map { it.fullRes.toImageUrl() }
        ),
        siteUrl = "${config.baseUrl}/game/${id}/${name.replace("[\\W]+".toRegex(), "-").toLowerCase()}"
    )

    private fun List<OpenCriticClient.Platform>.findReleaseDate(platform: Platform): String? =
        // OpenCritic returns all release dates for all platforms.
        find { it.id == platform.platformId }?.releaseDate?.toLocalDate()?.toString()

    private fun String.toImageUrl() = "http:$this"

    private val Platform.platformId: Int get() = config.getPlatformId(this)

    override val metadata = GameProvider.Metadata(
        id = id,
        logo = getResourceAsByteArray("opencritic.png"),
        supportedPlatforms = supportedPlatforms,
        defaultOrder = config.defaultOrder,
        accountFeature = null
    )

    override fun toString() = id

    companion object {
        const val id = "OpenCritic"
        val supportedPlatforms = listOf(Platform.Windows)
    }
}
