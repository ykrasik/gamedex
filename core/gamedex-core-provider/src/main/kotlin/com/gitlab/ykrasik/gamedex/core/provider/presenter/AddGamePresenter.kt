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

package com.gitlab.ykrasik.gamedex.core.provider.presenter

import com.gitlab.ykrasik.gamedex.LibraryPath
import com.gitlab.ykrasik.gamedex.app.api.provider.AddGamesView
import com.gitlab.ykrasik.gamedex.app.api.provider.GameSearchState
import com.gitlab.ykrasik.gamedex.app.api.provider.GameSearchStatus
import com.gitlab.ykrasik.gamedex.core.EventBus
import com.gitlab.ykrasik.gamedex.core.Presenter
import com.gitlab.ykrasik.gamedex.core.ViewSession
import com.gitlab.ykrasik.gamedex.core.library.LibraryService
import com.gitlab.ykrasik.gamedex.core.provider.GameSearchStartedEvent
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 16/04/2019
 * Time: 00:48
 */
@Singleton
class AddGamePresenter @Inject constructor(
    private val eventBus: EventBus,
    private val libraryService: LibraryService
) : Presenter<AddGamesView> {
    override fun present(view: AddGamesView) = object : ViewSession() {
        init {

        }

        override suspend fun onShow() {
            println("Sending event")
            eventBus.send(
                GameSearchStartedEvent(
                    GameSearchState(
                        index = 0,
                        libraryPath = LibraryPath(libraryService.libraries.first(), File(".")),
                        providerOrder = listOf("Metacritic", "Igdb", "GiantBomb"),
                        history = emptyMap(),
                        currentProvider = "Metacritic",
                        status = GameSearchStatus.Running,
                        game = null
                    ),
                    isAllowSmartChooseResults = true
                )
            )
        }
    }
}