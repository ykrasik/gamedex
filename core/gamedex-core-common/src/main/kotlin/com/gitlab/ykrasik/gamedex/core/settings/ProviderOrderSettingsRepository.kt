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

package com.gitlab.ykrasik.gamedex.core.settings

import com.gitlab.ykrasik.gamedex.app.api.settings.Order
import com.gitlab.ykrasik.gamedex.provider.GameProvider
import com.gitlab.ykrasik.gamedex.provider.defaultOrder
import com.gitlab.ykrasik.gamedex.provider.id
import com.gitlab.ykrasik.gamedex.util.Extractor

/**
 * User: ykrasik
 * Date: 24/06/2018
 * Time: 10:36
 */
class ProviderOrderSettingsRepository(factory: SettingsStorageFactory, providers: List<GameProvider>) :
    SettingsRepository<ProviderOrderSettingsRepository.Data>() {

    data class Data(
        val search: Order,
        val name: Order,
        val description: Order,
        val releaseDate: Order,
        val thumbnail: Order,
        val poster: Order,
        val screenshot: Order
    )

    override val storage = factory("order", Data::class) {
        fun defaultOrder(extractor: Extractor<GameProvider.OrderPriorities, Int>) =
            providers.sortedBy { extractor(it.defaultOrder) }.map { it.id }

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

    val searchChannel = storage.channel(Data::search)
    val search by searchChannel

    val nameChannel = storage.channel(Data::name)
    val name by nameChannel

    val descriptionChannel = storage.channel(Data::description)
    val description by descriptionChannel

    val releaseDateChannel = storage.channel(Data::releaseDate)
    val releaseDate by releaseDateChannel

    val thumbnailChannel = storage.channel(Data::thumbnail)
    val thumbnail by thumbnailChannel

    val posterChannel = storage.channel(Data::poster)
    val poster by posterChannel

    val screenshotChannel = storage.channel(Data::screenshot)
    val screenshot by screenshotChannel
}