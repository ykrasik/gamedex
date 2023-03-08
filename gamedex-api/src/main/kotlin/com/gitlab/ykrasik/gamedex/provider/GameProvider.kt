/****************************************************************************
 * Copyright (C) 2016-2023 Yevgeny Krasik                                   *
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

package com.gitlab.ykrasik.gamedex.provider

import com.gitlab.ykrasik.gamedex.GameData
import com.gitlab.ykrasik.gamedex.Platform
import com.gitlab.ykrasik.gamedex.Score

/**
 * User: ykrasik
 * Date: 29/05/2016
 * Time: 10:42
 */
interface GameProvider {
    val metadata: Metadata

    suspend fun search(query: String, platform: Platform, account: Account, offset: Int, limit: Int): SearchResponse
    suspend fun fetch(providerGameId: String, platform: Platform, account: Account): FetchResponse

    data class SearchResponse(
        val results: List<SearchResult>,
        val canShowMoreResults: Boolean?,  // Null means the provider doesn't know if it can or cannot show more results.
    )

    data class SearchResult(
        val providerGameId: String,
        val name: String,
        val description: String?,
        val releaseDate: String?,
        val criticScore: Score?,
        val userScore: Score?,
        val thumbnailUrl: String?,
    ) {
        override fun toString() = "GameProvider.SearchResult(providerGameId=$providerGameId, name=$name, releaseDate=$releaseDate)"
    }

    data class FetchResponse(
        val gameData: GameData,
        val siteUrl: String,
    )

    data class Metadata(
        val id: ProviderId,
        val logo: ByteArray,
        val supportedPlatforms: List<Platform>,
        val defaultOrder: OrderPriorities,
        val accountFeature: AccountFeature?,
    ) {
        fun supports(platform: Platform) = supportedPlatforms.contains(platform)

        override fun toString() = id
    }

    /**
     * Can have up to 4 fields (username, password, apikey etc).
     */
    interface AccountFeature {
        val accountUrl: String
        val field1: String? get() = null
        val field2: String? get() = null
        val field3: String? get() = null
        val field4: String? get() = null
        val fields: Int get() = listOfNotNull(field1, field2, field3, field4).size
        fun createAccount(fields: Map<String, String>): Account
    }

    interface Account {
        object Null : Account
    }

    // Lower number means higher priority
    data class OrderPriorities(
        val search: Int,
        val name: Int,
        val description: Int,
        val releaseDate: Int,
        val thumbnail: Int,
        val poster: Int,
        val screenshot: Int,
    ) {
        companion object {
            val default = OrderPriorities(
                search = 9999,
                name = 9999,
                description = 9999,
                releaseDate = 9999,
                thumbnail = 9999,
                poster = 9999,
                screenshot = 9999
            )
        }
    }
}

typealias ProviderId = String

val GameProvider.id get() = metadata.id