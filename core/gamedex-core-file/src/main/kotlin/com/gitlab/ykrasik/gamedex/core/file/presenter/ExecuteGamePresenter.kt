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

package com.gitlab.ykrasik.gamedex.core.file.presenter

import com.gitlab.ykrasik.gamedex.app.api.game.ViewCanExecuteGame
import com.gitlab.ykrasik.gamedex.core.Presenter
import com.gitlab.ykrasik.gamedex.core.ViewSession
import com.gitlab.ykrasik.gamedex.util.IsValid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.Desktop
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 01/05/2020
 * Time: 15:18
 */
@Singleton
class ExecuteGamePresenter @Inject constructor() : Presenter<ViewCanExecuteGame> {
    override fun present(view: ViewCanExecuteGame) = object : ViewSession() {
        private val game by view.gameChannel

        init {
            view.gameChannel.forEach { game ->
                view.canExecuteGame *= IsValid {
                    checkNotNull(game.mainExecutableFile) { "No file marked as main executable!" }
                }
            }
            view.executeGameActions.forEach {
                try {
                    view.canExecuteGame.assert()
                    withContext(Dispatchers.IO) {
                        // TODO: This is actually more like view-specific logic.
                        Desktop.getDesktop().open(game.mainExecutableFile)
                    }
                } catch (e: Exception) {
                    view.onError(message = e.message!!, title = "Error executing '${game.name}'", e = e)
                }
            }
        }
    }
}