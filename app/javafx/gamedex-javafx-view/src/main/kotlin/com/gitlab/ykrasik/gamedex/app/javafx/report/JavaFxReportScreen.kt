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
import com.gitlab.ykrasik.gamedex.app.api.filter.Filter
import com.gitlab.ykrasik.gamedex.app.api.game.ViewCanShowGameDetails
import com.gitlab.ykrasik.gamedex.app.api.report.*
import com.gitlab.ykrasik.gamedex.app.api.util.channel
import com.gitlab.ykrasik.gamedex.app.api.web.ViewCanBrowseUrl
import com.gitlab.ykrasik.gamedex.app.javafx.common.JavaFxCommonOps
import com.gitlab.ykrasik.gamedex.app.javafx.common.WebBrowser
import com.gitlab.ykrasik.gamedex.app.javafx.game.GameContextMenu
import com.gitlab.ykrasik.gamedex.app.javafx.game.details.JavaFxGameDetailsView
import com.gitlab.ykrasik.gamedex.javafx.*
import com.gitlab.ykrasik.gamedex.javafx.control.*
import com.gitlab.ykrasik.gamedex.javafx.theme.*
import com.gitlab.ykrasik.gamedex.javafx.view.PresentableScreen
import javafx.geometry.Pos
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
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
    ViewCanBrowsePath,
    ViewCanBrowseUrl {

    private val gameContextMenu: GameContextMenu by inject()
    private val browser = WebBrowser()

    private val commonOps: JavaFxCommonOps by di()

    override val excludeGameActions = channel<Game>()

    override val showGameDetailsActions = channel<Game>()
    override val editReportActions = channel<Report>()

    override val searchText = userMutableState("")

    override val matchingGame = state<Game?>(null)

    override val browsePathActions = channel<File>()
    override val browseUrlActions = channel<String>()

    override val report = userMutableState(Report.Null)
    override val result = state(ReportResult.Null)
    private val games = result.property.mapToList { it.games }

    private val gamesTable = tableview(games) {
        vgrow = Priority.ALWAYS
        fun isNotSelected(game: Game) = selectionModel.selectedItemProperty().isNotEqualTo(game)

        val indexColumn = makeIndexColumn().apply { addClass(CommonStyle.centered) }
        readonlyColumn("Game", Game::name)
        customGraphicColumn("Path") { game ->
            jfxButton(game.path.toString()) {
                mouseTransparentWhen { isNotSelected(game) }
                isFocusTraversable = false
                action(browsePathActions) { game.path }
            }
        }
        customGraphicColumn("Size", { game -> game.fileTree.value.size.toProperty() }) { size ->
            label(size.humanReadable)
        }.apply { minWidth = 60.0 }

        gameContextMenu.install(this) { selectionModel.selectedItem }
        onUserSelect { showGameDetailsActions.event(it) }

        result.property.onChange { resizeColumnsToFitContent() }

        minWidthFitContent(indexColumn)
    }

    private val selectedGame = gamesTable.selectionModel.selectedItemProperty()
    private val additionalInfoProperty = selectedGame.binding { result.value.additionalData[it?.id] }
    private val additionalInfo = additionalInfoProperty.mapToList { it?.toList() ?: emptyList() }

    private val additionalInfoView = tableview(additionalInfo) {
        makeIndexColumn().apply { addClass(CommonStyle.centered) }
//        simpleColumn("Rule") { result -> result.rule.name }
        customGraphicColumn("Value") {
            val result = it.value
            when (result) {
                is Filter.NameDiff.GameNameFolderDiff -> DiffResultFragment(result, selectedGame.value!!).root
                is Filter.Duplications.GameDuplication -> {
                    jfxButton {
                        useMaxSize = true
                        val game = games.find { it.id == result.duplicatedGameId }!!
                        text = game.name
                        action {
                            matchingGame.property.value = game
                        }
                    }
                }
                else -> label(result.toDisplayString())
            }
        }
        customGraphicColumn("Extra") {
            val result = it.value
            when (result) {
                is Filter.NameDiff.GameNameFolderDiff -> commonOps.providerLogo(result.providerId).toImageView(height = 80.0, width = 160.0)
                is Filter.Duplications.GameDuplication -> commonOps.providerLogo(result.providerId).toImageView(height = 80.0, width = 160.0)
                else -> label()
            }
        }

        additionalInfoProperty.onChange { additionalInfo ->
            // The selected game may not appear in the report (can happen with programmatic selection).
            items = additionalInfo?.toList()?.observable()
            resizeColumnsToFitContent()
        }
    }

    private val gameDetailsView = JavaFxGameDetailsView()

    override val root = hbox {
        vgrow = Priority.ALWAYS
        useMaxSize = true

        // Left
        vbox {
            hgrow = Priority.ALWAYS
            // Top
            add(gamesTable)

            // Bottom
            vbox(alignment = Pos.TOP_CENTER) {
                showWhen { additionalInfoProperty.booleanBinding { it?.isNotEmpty() ?: false } }
                header("Additional Info")
                add(additionalInfoView)
            }
        }

        gap()

        // Right
        vbox {
            hgrow = Priority.ALWAYS

            // Top
            addComponent(gameDetailsView) {
                root.paddingAll = 5.0
            }
            selectedGame.perform { game ->
                if (game != null) {
                    gameDetailsView.game.valueFromView = game
                }
            }

            verticalGap(size = 30)

            // Bottom
            addComponent(browser)
        }
    }

    init {
        register()

        matchingGame.property.typeSafeOnChange { match ->
            if (match != null) {
                gamesTable.selectionModel.select(match)
            }
        }

        selectedGame.onChange {
            browser.loadYouTubeGameplay(it)
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

        spacer()

        excludeButton {
            textProperty().bind(selectedGame.stringBinding { "Exclude ${it?.name ?: ""}" })
            enableWhen { selectedGame.isNotNull }
            action(excludeGameActions) { selectedGame.value!! }
        }
    }
}