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

package com.gitlab.ykrasik.gamedex.core.game

import com.gitlab.ykrasik.gamedex.app.api.util.ListChangeType
import com.gitlab.ykrasik.gamedex.app.api.util.task
import com.gitlab.ykrasik.gamedex.core.api.game.AddGameRequest
import com.gitlab.ykrasik.gamedex.core.api.game.GameRepository
import com.gitlab.ykrasik.gamedex.core.api.game.GameService
import com.gitlab.ykrasik.gamedex.core.api.library.LibraryService
import com.gitlab.ykrasik.gamedex.core.api.provider.GameProviderRepository
import kotlinx.coroutines.experimental.CommonPool
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 26/04/2018
 * Time: 19:51
 */
@Singleton
class GameServiceImpl @Inject constructor(
    private val repo: GameRepository,
    libraryService: LibraryService,
    gameProviderRepository: GameProviderRepository  // FIXME: Go through service!!!
) : GameService {
    init {
        libraryService.libraries.changesChannel.subscribe(CommonPool) {
            @Suppress("NON_EXHAUSTIVE_WHEN")
            when (it.type) {
                ListChangeType.Remove -> repo.invalidate()
                ListChangeType.Set -> repo.rebuildGames()
            }
        }
        gameProviderRepository.enabledProviders.changesChannel.subscribe(CommonPool) {
            repo.rebuildGames()
        }
    }

    override fun addAll(requests: List<AddGameRequest>) = task {
        message1 = "Adding ${requests.size} games..."
        totalWork = requests.size
        repo.addAll(requests) { incProgress() }
    }
}