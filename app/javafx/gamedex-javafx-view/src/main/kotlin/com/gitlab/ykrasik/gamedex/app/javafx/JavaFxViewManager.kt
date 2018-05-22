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

package com.gitlab.ykrasik.gamedex.app.javafx

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.GameDataType
import com.gitlab.ykrasik.gamedex.app.api.ViewManager
import com.gitlab.ykrasik.gamedex.app.javafx.game.delete.JavaFxDeleteGameView
import com.gitlab.ykrasik.gamedex.app.javafx.game.edit.JavaFxEditGameView
import com.gitlab.ykrasik.gamedex.app.javafx.game.tag.JavaFxTagGameView
import tornadofx.View
import tornadofx.vbox

/**
 * User: ykrasik
 * Date: 21/05/2018
 * Time: 10:37
 */
class JavaFxViewManager : View(), ViewManager {
    override val root = vbox()  // Unused.

    private val editGameView: JavaFxEditGameView by inject()
    private val tagGameView: JavaFxTagGameView by inject()

    override fun showEditGameView(game: Game, initialScreen: GameDataType) = editGameView.show(game, initialScreen)
    override fun showDeleteGameView(game: Game) = JavaFxDeleteGameView.show(game)
    override fun showTagGameView(game: Game) = tagGameView.show(game)
}