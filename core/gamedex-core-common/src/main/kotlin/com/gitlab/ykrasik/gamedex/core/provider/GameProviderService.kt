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

package com.gitlab.ykrasik.gamedex.core.provider

import com.gitlab.ykrasik.gamedex.Platform
import com.gitlab.ykrasik.gamedex.ProviderData
import com.gitlab.ykrasik.gamedex.ProviderHeader
import com.gitlab.ykrasik.gamedex.app.api.image.Image
import com.gitlab.ykrasik.gamedex.core.task.Task
import com.gitlab.ykrasik.gamedex.core.util.ListObservable
import com.gitlab.ykrasik.gamedex.provider.*

/**
 * User: ykrasik
 * Date: 13/10/2016
 * Time: 13:29
 */
interface GameProviderService {
    val allProviders: List<GameProvider>
    val enabledProviders: ListObservable<EnabledGameProvider>

    val platformsWithEnabledProviders: Set<Platform>

    fun isEnabled(id: ProviderId): Boolean

    val logos: Map<ProviderId, Image>

    fun verifyAccount(providerId: ProviderId, account: Map<String, String>): Task<Unit>

    fun search(providerId: ProviderId, query: String, platform: Platform): Task<List<ProviderSearchResult>>

    fun fetch(name: String, platform: Platform, headers: List<ProviderHeader>): Task<List<ProviderData>>
}

class EnabledGameProvider(private val provider: GameProvider, private val account: ProviderUserAccount) : GameProvider by provider {
    suspend fun search(query: String, platform: Platform): List<ProviderSearchResult> = provider.search(query, platform, account)
    suspend fun fetch(apiUrl: String, platform: Platform): ProviderFetchData = provider.fetch(apiUrl, platform, account)

    override fun toString() = provider.toString()
}

fun GameProviderService.assertHasEnabledProvider() = check(enabledProviders.isNotEmpty()) {
    "No providers are enabled! Please make sure there's at least 1 enabled provider in the settings menu."
}