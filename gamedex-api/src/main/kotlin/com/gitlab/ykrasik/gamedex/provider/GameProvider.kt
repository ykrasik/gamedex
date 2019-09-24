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
    val metadata: GameProviderMetadata

    suspend fun search(
        query: String,
        platform: Platform,
        account: ProviderUserAccount,
        offset: Int,
        limit: Int
    ): List<ProviderSearchResult>

    suspend fun fetch(
        providerGameId: String,
        platform: Platform,
        account: ProviderUserAccount
    ): ProviderFetchData
}

typealias ProviderId = String

data class GameProviderMetadata(
    val id: ProviderId,
    val logo: ByteArray,
    val supportedPlatforms: List<Platform>,
    val defaultOrder: ProviderOrderPriorities,
    val accountFeature: ProviderUserAccountFeature?
)

val GameProvider.id get() = metadata.id
val GameProvider.logo get() = metadata.logo
val GameProvider.supportedPlatforms get() = metadata.supportedPlatforms
val GameProvider.defaultOrder get() = metadata.defaultOrder
val GameProvider.accountFeature get() = metadata.accountFeature
fun GameProvider.supports(platform: Platform) = supportedPlatforms.contains(platform)

/**
 * Can have up to 4 fields (username, password, apikey etc).
 */
interface ProviderUserAccountFeature {
    val accountUrl: String
    val field1: String? get() = null
    val field2: String? get() = null
    val field3: String? get() = null
    val field4: String? get() = null
    val fields: Int get() = listOfNotNull(field1, field2, field3, field4).size
    fun createAccount(fields: Map<String, String>): ProviderUserAccount
}

interface ProviderUserAccount {
    object Null : ProviderUserAccount
}

// Higher number means lower priority
data class ProviderOrderPriorities(
    val search: Int,
    val name: Int,
    val description: Int,
    val releaseDate: Int,
    val criticScore: Int,
    val userScore: Int,
    val thumbnail: Int,
    val poster: Int,
    val screenshot: Int
) {
    companion object {
        val default = ProviderOrderPriorities(
            search = 9999,
            name = 9999,
            description = 9999,
            releaseDate = 9999,
            criticScore = 9999,
            userScore = 9999,
            thumbnail = 9999,
            poster = 9999,
            screenshot = 9999
        )
    }
}

data class ProviderSearchResult(
    val providerGameId: String,
    val name: String,
    val description: String?,
    val releaseDate: String?,
    val criticScore: Score?,
    val userScore: Score?,
    val thumbnailUrl: String?
) {
    override fun toString() = "ProviderSearchResult(providerGameId=$providerGameId, name=$name, releaseDate=$releaseDate)"
}

data class ProviderFetchData(
    val gameData: GameData,
    val siteUrl: String
)