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

package com.gitlab.ykrasik.gamedex.core.game.presenter.edit

import com.gitlab.ykrasik.gamedex.app.api.ViewManager
import com.gitlab.ykrasik.gamedex.app.api.game.ViewCanEditGame
import com.gitlab.ykrasik.gamedex.core.Presentation
import com.gitlab.ykrasik.gamedex.core.Presenter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 31/05/2018
 * Time: 10:22
 */
@Singleton
class ShowEditGamePresenter @Inject constructor(private val viewManager: ViewManager) : Presenter<ViewCanEditGame> {
    override fun present(view: ViewCanEditGame) = object : Presentation() {
        init {
            view.editGameActions.forEach { (game, initialScreen) ->
                viewManager.showEditGameView {
                    this.game = game
                    this.initialScreen = initialScreen
                }
            }
        }
    }
}