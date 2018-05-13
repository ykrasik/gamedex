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

package com.gitlab.ykrasik.gamedex.core.api.provider

import com.gitlab.ykrasik.gamedex.Platform
import com.gitlab.ykrasik.gamedex.ProviderData
import com.gitlab.ykrasik.gamedex.ProviderHeader
import com.gitlab.ykrasik.gamedex.app.api.image.Image
import com.gitlab.ykrasik.gamedex.app.api.util.ListObservable
import com.gitlab.ykrasik.gamedex.app.api.util.Task
import com.gitlab.ykrasik.gamedex.provider.GameProvider
import com.gitlab.ykrasik.gamedex.provider.ProviderId
import com.gitlab.ykrasik.gamedex.provider.ProviderSearchResult
import com.gitlab.ykrasik.gamedex.provider.ProviderUserAccount
import java.io.File

/**
 * User: ykrasik
 * Date: 04/04/2018
 * Time: 21:58
 */
// TODO: Make it stream results back?
interface GameProviderService {
    val allProviders: List<GameProvider>
    val enabledProviders: ListObservable<EnabledGameProvider>

    fun checkAtLeastOneProviderEnabled()

    fun provider(id: ProviderId): GameProvider
    fun isEnabled(id: ProviderId): Boolean

    val logos: Map<ProviderId, Image>

    // TODO: Split the methods here into GameDiscoveryService & GameDownloadService?
    fun search(name: String, platform: Platform, path: File, excludedProviders: List<ProviderId>): Task<SearchResults?>

    fun download(name: String, platform: Platform, path: File, headers: List<ProviderHeader>): Task<List<ProviderData>>
}

class EnabledGameProvider(private val provider: GameProvider, private val account: ProviderUserAccount?) : GameProvider by provider {
    fun search(name: String, platform: Platform): List<ProviderSearchResult> = provider.search(name, platform, account)
    fun download(apiUrl: String, platform: Platform): ProviderData = provider.download(apiUrl, platform, account)

    override fun toString() = provider.toString()
}

data class SearchResults(
    val providerData: List<ProviderData>,
    val excludedProviders: List<ProviderId>
) {
    fun isEmpty(): Boolean = providerData.isEmpty() && excludedProviders.isEmpty()
}