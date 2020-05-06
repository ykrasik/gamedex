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

import com.gitlab.ykrasik.gamedex.Platform
import com.gitlab.ykrasik.gamedex.app.api.game.AvailablePlatform
import com.gitlab.ykrasik.gamedex.app.api.game.GameDisplayType
import com.gitlab.ykrasik.gamedex.app.api.game.SortBy
import com.gitlab.ykrasik.gamedex.app.api.game.SortOrder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 01/05/2017
 * Time: 19:08
 */
@Singleton
class GameSettingsRepository @Inject constructor(repo: SettingsRepository) {
    data class Data(
        val platform: AvailablePlatform,
        val displayType: GameDisplayType,
        val sortBy: SortBy,
        val sortOrder: SortOrder,
        val maxGenres: Int,
        val maxScreenshots: Int
    )

    private val storage = repo.storage(basePath = "", name = "game") {
        Data(
            platform = AvailablePlatform.SinglePlatform(Platform.Windows),
            displayType = GameDisplayType.Wall,
            sortBy = SortBy.CriticScore,
            sortOrder = SortOrder.Desc,
            maxGenres = 7,
            maxScreenshots = 12
        )
    }

    val platform = storage.biChannel(Data::platform) { copy(platform = it) }
    val displayType = storage.biChannel(Data::displayType) { copy(displayType = it) }
    val sortBy = storage.biChannel(Data::sortBy) { copy(sortBy = it) }
    val sortOrder = storage.biChannel(Data::sortOrder) { copy(sortOrder = it) }
    val maxGenres = storage.biChannel(Data::maxGenres) { copy(maxGenres = it) }
    val maxScreenshots = storage.biChannel(Data::maxScreenshots) { copy(maxScreenshots = it) }
}