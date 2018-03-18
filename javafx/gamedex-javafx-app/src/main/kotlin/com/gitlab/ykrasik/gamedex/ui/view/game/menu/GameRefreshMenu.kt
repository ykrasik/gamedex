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

package com.gitlab.ykrasik.gamedex.ui.view.game.menu

import com.gitlab.ykrasik.gamedex.controller.GameController
import com.gitlab.ykrasik.gamedex.javafx.popOver
import com.gitlab.ykrasik.gamedex.javafx.CommonStyle
import com.gitlab.ykrasik.gamedex.javafx.refreshButton
import com.gitlab.ykrasik.gamedex.javafx.toggle
import javafx.scene.input.MouseEvent
import org.controlsfx.control.PopOver
import tornadofx.*

/**
 * User: ykrasik
 * Date: 05/06/2017
 * Time: 10:57
 */
class GameRefreshMenu : View() {
    private val gameController: GameController by di()

    private val staleDurationMenu: GameStaleDurationMenu by inject()

    override val root = refreshButton {
        enableWhen { gameController.canRunLongTask }

        val leftPopover = popOver(PopOver.ArrowLocation.RIGHT_TOP, closeOnClick = false) {
            children += staleDurationMenu.root
        }
        val downPopover = popOver {
            addEventFilter(MouseEvent.MOUSE_CLICKED) { leftPopover.hide() }
            disableWhen { staleDurationMenu.isFocused.or(staleDurationMenu.isValid.not()) }
            refreshButton("All Stale Games") {
                addClass(CommonStyle.fillAvailableWidth)
                tooltip("Refresh all games that were last refreshed before the stale duration")
                setOnAction { gameController.refreshAllGames() }
            }
            separator()
            refreshButton("Filtered Stale Games") {
                addClass(CommonStyle.fillAvailableWidth)
                tooltip("Refresh filtered games that were last refreshed before the stale duration")
                setOnAction { setOnAction { gameController.refreshFilteredGames() } }
            }
        }
        setOnAction { downPopover.toggle(this); leftPopover.toggle(this) }
    }
}