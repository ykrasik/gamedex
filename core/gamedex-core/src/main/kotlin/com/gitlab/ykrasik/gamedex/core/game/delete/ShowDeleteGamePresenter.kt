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

package com.gitlab.ykrasik.gamedex.core.game.delete

import com.gitlab.ykrasik.gamedex.app.api.ViewManager
import com.gitlab.ykrasik.gamedex.app.api.game.ViewCanDeleteGame
import com.gitlab.ykrasik.gamedex.core.Presentation
import com.gitlab.ykrasik.gamedex.core.Presenter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 01/06/2018
 * Time: 09:27
 */
@Singleton
class ShowDeleteGamePresenter @Inject constructor(private val viewManager: ViewManager) : Presenter<ViewCanDeleteGame> {
    override fun present(view: ViewCanDeleteGame) = object : Presentation() {
        init {
            view.deleteGameActions.forEach { game ->
                viewManager.showDeleteGameView {
                    this.game = game
                }
            }
        }
    }
}