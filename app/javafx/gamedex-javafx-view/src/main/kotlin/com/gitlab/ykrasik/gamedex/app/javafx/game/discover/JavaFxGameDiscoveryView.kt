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

package com.gitlab.ykrasik.gamedex.app.javafx.game.discover

import com.gitlab.ykrasik.gamedex.app.api.game.discover.ViewCanDiscoverGamesWithoutProviders
import com.gitlab.ykrasik.gamedex.app.api.game.discover.ViewCanDiscoverNewGames
import com.gitlab.ykrasik.gamedex.app.api.presenters
import com.gitlab.ykrasik.gamedex.javafx.popOver
import com.gitlab.ykrasik.gamedex.javafx.screen.PresentableView
import com.gitlab.ykrasik.gamedex.javafx.searchButton
import com.gitlab.ykrasik.gamedex.javafx.toggle
import javafx.geometry.Pos
import javafx.scene.input.MouseEvent
import org.controlsfx.control.PopOver
import tornadofx.enableWhen
import tornadofx.separator
import tornadofx.tooltip
import tornadofx.useMaxWidth

/**
 * User: ykrasik
 * Date: 05/06/2017
 * Time: 10:54
 */
class JavaFxGameDiscoveryView : PresentableView(), ViewCanDiscoverNewGames, ViewCanDiscoverGamesWithoutProviders {
    private val discoverNewGamesPresenter = presenters.discoverNewGames.present(this)
    private val discoverGamesWithoutProvidersPresenter = presenters.discoverGamesWithoutProviders.present(this)

    override val root = searchButton("Discover") {
        enableWhen { enabledProperty }
        // TODO: This is pretty ugly.
        val leftPopover = popOver(PopOver.ArrowLocation.RIGHT_TOP, closeOnClick = false) {
            discoverGameChooseResultsMenu()
        }
        val downPopover = popOver {
            addEventFilter(MouseEvent.MOUSE_CLICKED) { leftPopover.hide() }
            searchButton("New Games") {
                useMaxWidth = true
                alignment = Pos.CENTER_LEFT
                tooltip("Search all libraries for new games")
                onAction(discoverNewGamesPresenter::discoverNewGames)
            }
            separator()
            // TODO: Why did I put this here? What's the relation between refreshLibrary & this?
            searchButton("Games Without All Providers") {
                useMaxWidth = true
                alignment = Pos.CENTER_LEFT
                tooltip("Re-Discover all games that don't yet have all available providers")
                onAction(discoverGamesWithoutProvidersPresenter::discoverGamesWithoutProviders)
            }
        }
        setOnAction {
            leftPopover.toggle(this)
            downPopover.toggle(this)
        }
    }
}