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

package com.gitlab.ykrasik.gamedex.app.javafx.game.details

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.GameDataType
import com.gitlab.ykrasik.gamedex.app.api.game.GameView
import com.gitlab.ykrasik.gamedex.app.api.game.ViewCanDeleteGame
import com.gitlab.ykrasik.gamedex.app.api.game.ViewCanEditGame
import com.gitlab.ykrasik.gamedex.app.api.game.ViewCanTagGame
import com.gitlab.ykrasik.gamedex.app.api.image.Image
import com.gitlab.ykrasik.gamedex.app.api.provider.ViewCanRedownloadGame
import com.gitlab.ykrasik.gamedex.app.api.provider.ViewCanResyncGame
import com.gitlab.ykrasik.gamedex.app.api.util.channel
import com.gitlab.ykrasik.gamedex.app.api.web.ViewCanSearchYouTube
import com.gitlab.ykrasik.gamedex.app.javafx.image.ImageLoader
import com.gitlab.ykrasik.gamedex.javafx.*
import com.gitlab.ykrasik.gamedex.javafx.control.*
import com.gitlab.ykrasik.gamedex.javafx.theme.*
import com.gitlab.ykrasik.gamedex.javafx.view.PresentableScreen
import com.gitlab.ykrasik.gamedex.javafx.view.ScreenNavigation
import com.gitlab.ykrasik.gamedex.javafx.view.WebBrowser
import com.gitlab.ykrasik.gamedex.util.IsValid
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import kotlinx.coroutines.Deferred
import tornadofx.*

/**
 * User: ykrasik
 * Date: 30/03/2017
 * Time: 18:17
 */
class JavaFxViewGameScreen : PresentableScreen(), GameView, ViewCanEditGame, ViewCanDeleteGame,
    ViewCanTagGame, ViewCanResyncGame, ViewCanRedownloadGame, ViewCanSearchYouTube {
    override val navigation = ScreenNavigation.Standalone

    private val imageLoader: ImageLoader by di()

    private val browser = WebBrowser()

    override val game = userMutableState(Game.Null)

    override val poster = state<Deferred<Image>?>(null)

    override val displayYouTubeForGameRequests = channel<Game>()
    override val editGameActions = channel<Pair<Game, GameDataType>>()
    override val deleteGameActions = channel<Game>()
    override val tagGameActions = channel<Game>()
    override val redownloadGameActions = channel<Game>()

    override val canResyncGame = state(IsValid.valid)
    override val resyncGameActions = channel<Game>()

    private val gameDetailsView = JavaFxGameDetailsView(evenIfEmpty = true)

    init {
        register()

        game.property.bindChanges(displayYouTubeForGameRequests)
    }

    override fun HBox.buildToolbar() {
        editButton("Edit") { action { editGame(GameDataType.name_) } }
        gap()
        toolbarButton("Tag", graphic = Icons.tag) { action(tagGameActions) { game.value } }
        gap()
        deleteButton("Delete") { action(deleteGameActions) { game.value } }

        spacer()

        infoButton("Re-Download", graphic = Icons.download) { action(redownloadGameActions) { game.value } }
        gap()
        infoButton("Re-Sync", graphic = Icons.sync) {
            enableWhen(canResyncGame)
            action(resyncGameActions) { game.value }
        }
    }

    override val root = hbox {
        paddingAll = 8

        // Left
        stackpane {
            addClass(CommonStyle.card)

            popoverContextMenu {
                jfxButton("Change Poster", graphic = Icons.poster) {
                    action { editGame(GameDataType.poster) }
                }
            }

            imageViewResizingPane(poster.property.flatMap { imageLoader.loadImage(it) }) {
                maxWidth = screenBounds.width * maxPosterWidthPercent
                clipRectangle(arc = 20)
            }
        }

        gap(size = 8)

        // Right
        vbox {
            addClass(CommonStyle.card)
            paddingAll = 10
            hgrow = Priority.ALWAYS

            // Top
            addComponent(gameDetailsView)
            game.onChange {
                gameDetailsView.game.valueFromView = it
            }

            verticalGap(size = 30)

            // Bottom
            addComponent(browser)
        }
    }

    private fun editGame(initialTab: GameDataType) = editGameActions.event(game.value to initialTab)

    override fun browseTo(url: String?) = browser.load(url)

    companion object {
        private val maxPosterWidthPercent = 0.44
    }
}