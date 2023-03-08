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

package com.gitlab.ykrasik.gamedex.core.file.presenter

import com.gitlab.ykrasik.gamedex.app.api.game.ViewCanLaunchGame
import com.gitlab.ykrasik.gamedex.core.view.Presenter
import com.gitlab.ykrasik.gamedex.core.view.ViewSession
import com.gitlab.ykrasik.gamedex.util.IsValid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
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
class LaunchGamePresenter @Inject constructor() : Presenter<ViewCanLaunchGame> {
    override fun present(view: ViewCanLaunchGame) = object : ViewSession() {
        init {
            view::canLaunchGame *= view.game.onlyChangesFromView().map { game ->
                withContext(Dispatchers.IO) {
                    IsValid {
                        val mainExecutableFile = checkNotNull(game.mainExecutableFile) { "No file marked as main executable!" }
                        check(mainExecutableFile.exists()) { "Main Executable File doesn't exit!" }
                    }
                }
            }
            view::launchGameActions.forEach {
                val game = view.game.v
                try {
                    view.canLaunchGame.assert()
                    withContext(Dispatchers.IO) {
                        // TODO: This is actually more like view-specific logic.
                        Desktop.getDesktop().open(game.mainExecutableFile)
                    }
                } catch (e: Exception) {
                    // FIXME: This should happen by default to all views.
                    view.onError(message = e.message!!, title = "Error executing '${game.name}'", e = e)
                }
            }
        }
    }
}