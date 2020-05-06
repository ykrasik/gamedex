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

package com.gitlab.ykrasik.gamedex.core.settings

import com.gitlab.ykrasik.gamedex.app.api.settings.Order
import com.gitlab.ykrasik.gamedex.core.provider.GameProviderService
import com.gitlab.ykrasik.gamedex.provider.GameProvider
import com.gitlab.ykrasik.gamedex.util.Extractor
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 24/06/2018
 * Time: 10:36
 */
@Singleton
class ProviderOrderSettingsRepository @Inject constructor(
    repo: SettingsRepository,
    gameProviderService: GameProviderService
) {
    data class Data(
        val search: Order,
        val name: Order,
        val description: Order,
        val releaseDate: Order,
        val thumbnail: Order,
        val poster: Order,
        val screenshot: Order
    )

    private val storage = repo.storage(basePath = "provider", name = "order") {
        fun defaultOrder(extractor: Extractor<GameProvider.OrderPriorities, Int>) =
            gameProviderService.allProviders.sortedBy { extractor(it.defaultOrder) }.map { it.id }

        Data(
            search = defaultOrder { search },
            name = defaultOrder { name },
            description = defaultOrder { description },
            releaseDate = defaultOrder { releaseDate },
            thumbnail = defaultOrder { thumbnail },
            poster = defaultOrder { poster },
            screenshot = defaultOrder { screenshot }
        )
    }

    fun onChange(f: suspend () -> Unit) = storage.onChange { f() }

    val search = storage.biChannel(Data::search) { copy(search = it) }
    val name = storage.biChannel(Data::name) { copy(name = it) }
    val description = storage.biChannel(Data::description) { copy(description = it) }
    val releaseDate = storage.biChannel(Data::releaseDate) { copy(releaseDate = it) }
    val thumbnail = storage.biChannel(Data::thumbnail) { copy(thumbnail = it) }
    val poster = storage.biChannel(Data::poster) { copy(poster = it) }
    val screenshot = storage.biChannel(Data::screenshot) { copy(screenshot = it) }
}