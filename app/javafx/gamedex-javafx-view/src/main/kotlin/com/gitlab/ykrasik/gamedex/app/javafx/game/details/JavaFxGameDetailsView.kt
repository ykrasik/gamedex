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
import com.gitlab.ykrasik.gamedex.app.api.file.ViewCanOpenFile
import com.gitlab.ykrasik.gamedex.app.api.game.*
import com.gitlab.ykrasik.gamedex.app.api.provider.ViewCanRefetchGame
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
class JavaFxGameDetailsView : PresentableScreen(),
    GameDetailsView,
    ViewCanEditGame,
    ViewCanDeleteGame,
    ViewCanRenameMoveGame,
    ViewCanTagGame,
    ViewCanRefetchGame,
    ViewCanResyncGame,
    ViewCanOpenFile {

    private val commonOps: JavaFxCommonOps by di()

    private val browser = WebBrowser()

    private val isBrowserVisibleProperty = SimpleBooleanProperty(false)
    private var isBrowserVisible by isBrowserVisibleProperty

    override val gameParams = userMutableState(ViewGameParams(Game.Null))
    private val game = gameParams.property.binding { it.game }

    override val hideViewActions = channel<Unit>()
    override val customNavigationButton = backButton { action(hideViewActions) }

    override val editGameActions = channel<EditGameParams>()
    override val deleteGameActions = channel<Game>()
    override val renameMoveGameActions = channel<RenameMoveGameParams>()
    override val tagGameActions = channel<Game>()
    override val refetchGameActions = channel<Game>()

    override val canResyncGame = state(IsValid.valid)
    override val resyncGameActions = channel<Game>()

    override val openFileActions = channel<File>()
    private val browseUrlActions = channel<String>()

    init {
        register()

        titleProperty.bind(game.stringBinding { it?.name })
        game.typeSafeOnChange { game ->
            isBrowserVisible = game.id != Game.Null.id
            if (isBrowserVisible) {
                browser.loadYouTubeGameplay(game)
            }
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
            tooltip(game.stringBinding { "Search YouTube for gameplay videos of '${it?.name}'" })
            action {
                isBrowserVisible = true
                browser.loadYouTubeGameplay(game.value)
            }
        }

        spacer()
        editButton("Edit") { action(editGameActions) { EditGameParams(game.value, initialView = GameDataType.Name) } }
        gap()
        toolbarButton("Tag", Icons.tag) { action(tagGameActions) { game.value } }
        gap()
        extraMenu {
            infoButton("Re-Fetch", graphic = Icons.download) {
                useMaxWidth = true
                alignment = Pos.CENTER_LEFT
                action(refetchGameActions) { game.value }
            }
            infoButton("Re-Sync", graphic = Icons.sync) {
                useMaxWidth = true
                alignment = Pos.CENTER_LEFT
                enableWhen(canResyncGame)
                action(resyncGameActions) { game.value }
            }

            verticalGap()

            warningButton("Rename/Move Folder", Icons.folderEdit) {
                action(renameMoveGameActions) { RenameMoveGameParams(game.value, initialSuggestion = null) }
            }
            deleteButton("Delete") {
                useMaxWidth = true
                alignment = Pos.CENTER_LEFT
                action(deleteGameActions) { game.value }
            }
        }
    }

    override val root = stackpane {
        vgrow = Priority.ALWAYS
        stackpane {
            backgroundProperty().bind(game.flatMap { game ->
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
                game.onChange { game ->
                    replaceChildren {
                        if (game!!.id != Game.Null.id) {
                            children += GameDetailsSummaryBuilder(game, commonOps) {
                                browsePathActions = this@JavaFxGameDetailsView.openFileActions
                                browseUrlActions = this@JavaFxGameDetailsView.browseUrlActions
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
}