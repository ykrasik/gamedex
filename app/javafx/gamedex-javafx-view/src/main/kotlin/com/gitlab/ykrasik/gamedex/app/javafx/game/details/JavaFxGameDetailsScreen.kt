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

package com.gitlab.ykrasik.gamedex.app.javafx.game.details

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.GameDataType
import com.gitlab.ykrasik.gamedex.app.api.file.ViewCanBrowsePath
import com.gitlab.ykrasik.gamedex.app.api.game.GameDetailsView
import com.gitlab.ykrasik.gamedex.app.api.game.ViewCanDeleteGame
import com.gitlab.ykrasik.gamedex.app.api.game.ViewCanEditGame
import com.gitlab.ykrasik.gamedex.app.api.game.ViewCanTagGame
import com.gitlab.ykrasik.gamedex.app.api.provider.ViewCanRedownloadGame
import com.gitlab.ykrasik.gamedex.app.api.provider.ViewCanResyncGame
import com.gitlab.ykrasik.gamedex.app.api.util.channel
import com.gitlab.ykrasik.gamedex.app.javafx.common.JavaFxCommonOps
import com.gitlab.ykrasik.gamedex.app.javafx.common.WebBrowser
import com.gitlab.ykrasik.gamedex.app.javafx.report.JavaFxReportScreen
import com.gitlab.ykrasik.gamedex.javafx.*
import com.gitlab.ykrasik.gamedex.javafx.control.*
import com.gitlab.ykrasik.gamedex.javafx.theme.*
import com.gitlab.ykrasik.gamedex.javafx.view.PresentableScreen
import com.gitlab.ykrasik.gamedex.util.IsValid
import javafx.beans.property.SimpleBooleanProperty
import javafx.geometry.Pos
import javafx.scene.layout.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import tornadofx.*
import java.io.File

/**
 * User: ykrasik
 * Date: 30/03/2017
 * Time: 18:17
 */
class JavaFxGameDetailsScreen : PresentableScreen(),
    GameDetailsView,
    ViewCanEditGame,
    ViewCanDeleteGame,
    ViewCanTagGame,
    ViewCanResyncGame,
    ViewCanRedownloadGame,
    ViewCanBrowsePath {

    private val commonOps: JavaFxCommonOps by di()

    private val browser = WebBrowser()

    private val isBrowserVisibleProperty = SimpleBooleanProperty(false)
    private var isBrowserVisible by isBrowserVisibleProperty

    override val game = userMutableState<Game?>(null)

    override val hideViewActions = channel<Unit>()
    override val customNavigationButton = backButton { action(hideViewActions) }

    override val editGameActions = channel<Pair<Game, GameDataType>>()
    override val deleteGameActions = channel<Game>()
    override val tagGameActions = channel<Game>()
    override val redownloadGameActions = channel<Game>()

    override val canResyncGame = state(IsValid.valid)
    override val resyncGameActions = channel<Game>()

    override val browsePathActions = channel<File>()
    private val browseUrlActions = channel<String>()

    init {
        register()

        titleProperty.bind(game.property.stringBinding { it?.name })
        game.property.typeSafeOnChange { game ->
            isBrowserVisible = game != null
            browser.loadYouTubeGameplay(game)
        }

        GlobalScope.launch(Dispatchers.JavaFx) {
            browseUrlActions.consumeEach { url ->
                browser.load(url)
            }
        }
    }

    override fun HBox.buildToolbar() {
        jfxToggleNode("Browser") {
            selectedProperty().bindBidirectional(isBrowserVisibleProperty)
            graphicProperty().bind(selectedProperty().binding { if (it) Icons.earthOff else Icons.earth })
            tooltip(selectedProperty().binding { "${if (it) "Hide" else "Show"} Browser" })
        }
        jfxButton("YouTube", Icons.youTube) {
            tooltip(game.property.stringBinding { "Search YouTube for gameplay videos of '${it?.name}'" })
            action {
                isBrowserVisible = true
                browser.loadYouTubeGameplay(game.value)
            }
        }

        spacer()
        editButton("Edit") { action { editGame(GameDataType.Name) } }
        gap()
        toolbarButton("Tag", graphic = Icons.tag) { action(tagGameActions) { game.value!! } }
        gap()
        extraMenu {
            deleteButton("Delete") {
                useMaxWidth = true
                alignment = Pos.CENTER_LEFT
                action(deleteGameActions) { game.value!! }
            }
            infoButton("Re-Download", graphic = Icons.download) {
                useMaxWidth = true
                alignment = Pos.CENTER_LEFT
                action(redownloadGameActions) { game.value!! }
            }
            infoButton("Re-Sync", graphic = Icons.sync) {
                useMaxWidth = true
                alignment = Pos.CENTER_LEFT
                enableWhen(canResyncGame)
                action(resyncGameActions) { game.value!! }
            }
        }
    }

    override val root = stackpane {
        vgrow = Priority.ALWAYS
        stackpane {
            backgroundProperty().bind(game.property.flatMap { game ->
                val image = commonOps.fetchPoster(game)
                image.binding {
                    Background(
                        BackgroundImage(
                            it,
                            BackgroundRepeat.NO_REPEAT,
                            BackgroundRepeat.NO_REPEAT,
                            BackgroundPosition.CENTER,
                            BackgroundSize(1.0, 1.0, true, true, true, false)
                        )
                    )
                }
            })
        }
        vbox {
            paddingAll = 40.0
            vgrow = Priority.ALWAYS
            alignment = Pos.TOP_CENTER
            addClass(JavaFxReportScreen.Style.detailsViewContent)

            // Top
            stackpane {
                paddingAll = 5.0
                game.property.onChange { game ->
                    replaceChildren {
                        if (game != null) {
                            children += GameDetailsPaneBuilder(game, commonOps) {
                                browsePathActions = this@JavaFxGameDetailsScreen.browsePathActions
                                browseUrlActions = this@JavaFxGameDetailsScreen.browseUrlActions
                                fillWidth = false
                                imageFitWidth = 400
                                imageFitHeight = 400
                            }.build {
                                alignment = Pos.TOP_CENTER
                            }
                        }
                    }
                }
            }

            verticalGap(size = 30)

            // Bottom
            addComponent(browser) {
                root.visibleWhen { isBrowserVisibleProperty }
            }
        }
    }

    private fun editGame(initialTab: GameDataType) = editGameActions.event(game.value!! to initialTab)
}