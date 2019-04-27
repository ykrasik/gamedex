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

package com.gitlab.ykrasik.gamedex.app.javafx.report

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.app.api.file.ViewCanBrowsePath
import com.gitlab.ykrasik.gamedex.app.api.game.ViewCanShowGameDetails
import com.gitlab.ykrasik.gamedex.app.api.report.*
import com.gitlab.ykrasik.gamedex.app.api.util.channel
import com.gitlab.ykrasik.gamedex.app.javafx.common.JavaFxCommonOps
import com.gitlab.ykrasik.gamedex.app.javafx.common.WebBrowser
import com.gitlab.ykrasik.gamedex.app.javafx.game.GameContextMenu
import com.gitlab.ykrasik.gamedex.app.javafx.game.details.GameDetailsPaneBuilder
import com.gitlab.ykrasik.gamedex.javafx.*
import com.gitlab.ykrasik.gamedex.javafx.control.*
import com.gitlab.ykrasik.gamedex.javafx.theme.Colors
import com.gitlab.ykrasik.gamedex.javafx.theme.Icons
import com.gitlab.ykrasik.gamedex.javafx.theme.excludeButton
import com.gitlab.ykrasik.gamedex.javafx.theme.header
import com.gitlab.ykrasik.gamedex.javafx.view.PresentableScreen
import javafx.beans.property.SimpleBooleanProperty
import javafx.geometry.Orientation
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
 * Date: 10/06/2017
 * Time: 16:25
 */
class JavaFxReportScreen : PresentableScreen("Reports", Icons.chart),
    ReportView,
    ViewCanExcludeGameFromReport,
    ViewCanSearchReportResult,
    ViewCanShowGameDetails,
    ViewCanEditReport,
    ViewCanBrowsePath {

    private val gameContextMenu: GameContextMenu by inject()
    private val browser = WebBrowser()

    private val isBrowserVisibleProperty = SimpleBooleanProperty(false)
    private var isBrowserVisible by isBrowserVisibleProperty

    private val commonOps: JavaFxCommonOps by di()

    override val excludeGameActions = channel<Game>()

    override val showGameDetailsActions = channel<Game>()
    override val editReportActions = channel<Report>()

    override val searchText = userMutableState("")

    override val matchingGame = state<Game?>(null)

    override val browsePathActions = channel<File>()
    private val browseUrlActions = channel<String>()

    override val report = userMutableState(Report.Null)
    override val result = state(ReportResult.Null)
    private val games = result.property.mapToList { it.games }

    private val gamesView = prettyListView(games) {
        vgrow = Priority.ALWAYS
        useMaxSize = true

        prettyListCell { game ->
            text = null
            maxWidth = 600.0
            graphic = GameDetailsPaneBuilder(
                name = game.name,
                nameOp = { isWrapText = true },
                platform = game.platform,
                releaseDate = game.releaseDate,
                criticScore = game.criticScore,
                userScore = game.userScore,
                path = game.path,
                fileTree = game.fileTree,
                image = commonOps.fetchThumbnail(game),
                browsePathActions = browsePathActions,
                pathOp = { isMouseTransparent = true },
                imageFitHeight = 70,
                imageFitWidth = 70,
                orientation = Orientation.HORIZONTAL
            ).build()
//            val cellMinWidth = prefWidth(-1.0) + (verticalScrollbar?.width ?: 0.0) + insets.left + insets.right
//            if (cellMinWidth > this@customListView.minWidth) {
//                this@customListView.minWidth = cellMinWidth
//            }
        }

        gameContextMenu.install(this) { selectionModel.selectedItem }
        onUserSelect { showGameDetailsActions.event(it) }
    }

    private val selectedGame = gamesView.selectionModel.selectedItemProperty()

    override val root = hbox {
        vgrow = Priority.ALWAYS
        useMaxSize = true

        // Left
        vbox {
            hgrow = Priority.ALWAYS
            minWidth = width
            add(gamesView)
        }

        // Right
        vbox {
            hgrow = Priority.ALWAYS

            GlobalScope.launch(Dispatchers.JavaFx) {
                browseUrlActions.consumeEach { url ->
                    isBrowserVisible = true
                    browser.load(url)
                }
            }

            customToolbar {
                jfxToggleNode("Browser") {
                    selectedProperty().bindBidirectional(isBrowserVisibleProperty)
                    graphicProperty().bind(selectedProperty().binding { if (it) Icons.earthOff else Icons.earth })
                    tooltip(selectedProperty().binding { "${if (it) "Hide" else "Show"} Browser" })
                }
                jfxButton("YouTube", Icons.youTube) {
                    enableWhen { selectedGame.isNotNull }
                    tooltip(selectedGame.stringBinding { "Search YouTube for gameplay videos of '${it?.name}'" })
                    action {
                        isBrowserVisible = true
                        browser.loadYouTubeGameplay(selectedGame.value)
                    }
                }

                spacer()

                excludeButton {
                    action(excludeGameActions) { selectedGame.value!! }
                    tooltip(selectedGame.mapWith(report.property) { game, report ->
                        "Exclude game '${game?.name}' from report '${report.name}' - it will not show in results any more."
                    })
                }
            }

            stackpane {
                vgrow = Priority.ALWAYS
                stackpane {
                    backgroundProperty().bind(selectedGame.flatMap { game ->
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
                    addClass(Style.detailsViewContent)

                    // Top
                    stackpane {
                        paddingAll = 5.0
                        selectedGame.onChange { game ->
                            isBrowserVisible = false
                            replaceChildren {
                                if (game != null) {
                                    children += GameDetailsPaneBuilder(game, commonOps) {
                                        browsePathActions = this@JavaFxReportScreen.browsePathActions
                                        browseUrlActions = this@JavaFxReportScreen.browseUrlActions
                                        fillWidth = false
                                        imageFitWidth = 320
                                        imageFitHeight = 320
                                        descriptionOp = { maxWidth = 600.0 }
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
    }

    init {
        register()

        matchingGame.property.typeSafeOnChange { match ->
            if (match != null) {
                gamesView.selectionModel.select(match)
            }
        }

        report.property.typeSafeOnChange {
            isBrowserVisible = false
            browser.load(null)
        }

        whenUndocked {
            isBrowserVisible = false
            browser.load(null)
        }
    }

    override fun HBox.buildToolbar() {
        jfxButton {
            textProperty().bind(report.property.stringBinding { it!!.name })
            graphicProperty().bind(hoverProperty().binding { if (it) Icons.edit else Icons.chart })
            action(editReportActions) { report.value }
        }
        gap()
        header(games.sizeProperty.stringBinding { "Games: $it" })
        gap()
        searchTextField(this@JavaFxReportScreen, searchText.property) { isFocusTraversable = false }
    }

    class Style : Stylesheet() {
        companion object {
            val detailsViewContent by cssclass()

            init {
                importStylesheetSafe(Style::class)
            }
        }

        init {
            detailsViewContent {
                backgroundColor = multi(Colors.transparentWhite)
            }
        }
    }
}