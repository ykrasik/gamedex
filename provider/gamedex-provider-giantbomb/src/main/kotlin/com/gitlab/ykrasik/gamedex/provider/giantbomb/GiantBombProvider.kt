/****************************************************************************
 * Copyright (C) 2016-2018 Yevgeny Krasik                                   *
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

import com.gitlab.ykrasik.gamedex.*
import com.gitlab.ykrasik.gamedex.provider.GameProvider
import com.gitlab.ykrasik.gamedex.provider.ProviderSearchResult
import com.gitlab.ykrasik.gamedex.provider.ProviderUserAccount
import com.gitlab.ykrasik.gamedex.provider.ProviderUserAccountFeature
import com.gitlab.ykrasik.gamedex.util.debug
import com.gitlab.ykrasik.gamedex.util.getResourceAsByteArray
import com.gitlab.ykrasik.gamedex.util.logger
import com.gitlab.ykrasik.gamedex.util.now
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 29/05/2016
 * Time: 10:53
 */
@Singleton
class GiantBombProvider @Inject constructor(private val config: GiantBombConfig, private val client: GiantBombClient) : GameProvider {
    private val log = logger()

    override fun search(name: String, platform: Platform, account: ProviderUserAccount?): List<ProviderSearchResult> {
        log.debug { "[$platform] Searching: '$name'..." }
        val response = client.search(name, platform, account as GiantBombUserAccount)
        assertOk(response.statusCode)

        val results = response.results.map { it.toProviderSearchResult() }
        log.debug { "[$platform] Searching: '$name': ${results.size} results." }
        results.forEach { log.trace(it.toString()) }
        return results
    }

    private fun GiantBombClient.SearchResult.toProviderSearchResult() = ProviderSearchResult(
        name = name,
        releaseDate = originalReleaseDate?.toString(),
        criticScore = null,
        userScore = null,
        thumbnailUrl = image?.thumbUrl?.filterEmptyImage(),
        apiUrl = apiDetailUrl
    )

    override fun download(apiUrl: String, platform: Platform, account: ProviderUserAccount?): ProviderData {
        log.debug { "[$platform] Downloading: $apiUrl..." }
        val response = client.fetch(apiUrl, account as GiantBombUserAccount)
        assertOk(response.statusCode)

        // When result is found - GiantBomb returns a Json object.
        // When result is not found, GiantBomb returns an empty Json array [].
        // So 'results' can contain at most a single value.
        val gameData = response.results.first().toProviderData(apiUrl)
        log.debug { "[$platform] Result: $gameData." }
        return gameData
    }

    private fun GiantBombClient.DetailsResult.toProviderData(apiUrl: String) = ProviderData(
        header = ProviderHeader(
            id = id,
            apiUrl = apiUrl,
            updateDate = now
        ),
        gameData = GameData(
            siteUrl = this.siteDetailUrl,
            name = this.name,
            description = this.deck,
            releaseDate = this.originalReleaseDate?.toString(),
            criticScore = null,
            userScore = null,
            genres = this.genres?.map { it.name } ?: emptyList(),
            imageUrls = ImageUrls(
                thumbnailUrl = this.image?.thumbUrl?.filterEmptyImage(),
                posterUrl = this.image?.superUrl?.filterEmptyImage(),
                screenshotUrls = this.images.mapNotNull { it.superUrl.filterEmptyImage() }
            )
        )
    )

    private fun String.filterEmptyImage(): String? = if (endsWith(config.noImageFileName)) null else this

    private fun assertOk(status: GiantBombClient.Status) {
        if (status != GiantBombClient.Status.ok) {
            throw IllegalStateException("Invalid statusCode: $status")
        }
    }

    override val id = "GiantBomb"
    override val logo = getResourceAsByteArray("giantbomb.png")
    override val supportedPlatforms = Platform.values().toList()
    override val defaultOrder = config.defaultOrder
    override val accountFeature = object : ProviderUserAccountFeature {
        override val accountUrl = config.accountUrl
        override val field1 = "Api Key"
        override fun createAccount(fields: Map<String, String>) = GiantBombUserAccount(
            apiKey = fields[field1]!!
        )
    }

    override fun toString() = id
}