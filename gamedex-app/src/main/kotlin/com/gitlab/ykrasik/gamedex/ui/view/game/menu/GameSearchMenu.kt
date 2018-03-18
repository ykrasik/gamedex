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
import com.gitlab.ykrasik.gamedex.ui.popOver
import com.gitlab.ykrasik.gamedex.ui.theme.CommonStyle
import com.gitlab.ykrasik.gamedex.ui.theme.searchButton
import com.gitlab.ykrasik.gamedex.ui.toggle
import javafx.scene.input.MouseEvent
import org.controlsfx.control.PopOver
import tornadofx.*

/**
 * User: ykrasik
 * Date: 05/06/2017
 * Time: 10:54
 */
class GameSearchMenu : View() {
    private val gameController: GameController by di()

    override val root = searchButton {
        enableWhen { gameController.canRunLongTask }
        val leftPopover = popOver(PopOver.ArrowLocation.RIGHT_TOP, closeOnClick = false) {
            ChooseSearchResultsToggleMenu().install(this)
        }
        val downPopover = popOver {
            addEventFilter(MouseEvent.MOUSE_CLICKED) { leftPopover.hide() }
            searchButton("New Games") {
                addClass(CommonStyle.fillAvailableWidth)
                tooltip("Search all libraries for new games")
                setOnAction { gameController.scanNewGames() }
            }
            separator()
            searchButton("All Games Without All Providers") {
                addClass(CommonStyle.fillAvailableWidth)
                tooltip("Search all games that don't already have all available providers")
                setOnAction { gameController.rediscoverAllGamesWithoutAllProviders() }
            }
            separator()
            searchButton("Filtered Games Without All Providers") {
                addClass(CommonStyle.fillAvailableWidth)
                tooltip("Search currently filtered games that don't already have all available providers")
                setOnAction { gameController.rediscoverFilteredGamesWithoutAllProviders() }
            }
        }
        setOnAction { downPopover.toggle(this); leftPopover.toggle(this) }
    }
}