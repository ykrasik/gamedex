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
import com.gitlab.ykrasik.gamedex.app.api.file.ViewCanBrowseFile
import com.gitlab.ykrasik.gamedex.app.api.filter.Filter
import com.gitlab.ykrasik.gamedex.app.api.game.ViewCanShowGameDetails
import com.gitlab.ykrasik.gamedex.app.api.image.Image
import com.gitlab.ykrasik.gamedex.app.api.provider.ViewWithProviderLogos
import com.gitlab.ykrasik.gamedex.app.api.report.*
import com.gitlab.ykrasik.gamedex.app.api.util.channel
import com.gitlab.ykrasik.gamedex.app.api.web.ViewCanSearchYouTube
import com.gitlab.ykrasik.gamedex.app.javafx.game.GameContextMenu
import com.gitlab.ykrasik.gamedex.app.javafx.game.details.JavaFxGameDetailsView
import com.gitlab.ykrasik.gamedex.app.javafx.image.image
import com.gitlab.ykrasik.gamedex.javafx.*
import com.gitlab.ykrasik.gamedex.javafx.control.*
import com.gitlab.ykrasik.gamedex.javafx.theme.*
import com.gitlab.ykrasik.gamedex.javafx.view.PresentableScreen
import com.gitlab.ykrasik.gamedex.javafx.view.WebBrowser
import com.gitlab.ykrasik.gamedex.provider.ProviderId
import javafx.beans.value.ObservableValue
import javafx.event.EventTarget
import javafx.geometry.Pos
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
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
    ViewWithProviderLogos,
    ViewCanSearchYouTube,
    ViewCanBrowseFile {

    private val gameContextMenu: GameContextMenu by inject()
    private val browser = WebBrowser()

    override var providerLogos = emptyMap<ProviderId, Image>()

    override val report = userMutableState(Report.Null)
    override val result = state(ReportResult.Null)

    override val excludeGameActions = channel<Game>()

    private val games = result.property.mapToList { it.games }

    override val showGameDetailsActions = channel<Game>()
    override val editReportActions = channel<Report>()

    override val searchText = userMutableState("")

    override val matchingGame = state<Game?>(null)

    override val displayYouTubeForGameRequests = channel<Game>()

    private val gamesTable = tableview(games) {
        vgrow = Priority.ALWAYS
        fun isNotSelected(game: Game) = selectionModel.selectedItemProperty().isNotEqualTo(game)

        val indexColumn = makeIndexColumn().apply { addClass(CommonStyle.centered) }
        readonlyColumn("Game", Game::name)
        customGraphicColumn("Path") { game ->
            jfxButton(game.path.toString()) {
                mouseTransparentWhen { isNotSelected(game) }
                isFocusTraversable = false
                action(browseToFileActions) { game.path }
            }
        }
        customGraphicColumn("Size", { game -> game.fileStructure.value.size.toProperty() }) { size ->
            label(size.humanReadable)
        }.apply { minWidth = 60.0 }

        gameContextMenu.install(this) { selectionModel.selectedItem }
        onUserSelect { showGameDetailsActions.event(it) }

        result.property.onChange { resizeColumnsToFitContent() }

        minWidthFitContent(indexColumn)

        selectionModel.selectedItemProperty().onChange {
            if (it != null) {
                displayYouTubeForGameRequests.event(it)
            }
        }
    }

    private val selectedGameProperty = gamesTable.selectionModel.selectedItemProperty()

    private val additionalInfoProperty = selectedGameProperty.binding { result.value.additionalData[it?.id] }

    override val browseToFileActions = channel<File>()

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
            container("Additional Info".toProperty()) {
                showWhen { additionalInfoProperty.booleanBinding { it?.isNotEmpty() ?: false } }
                additionalInfoView()
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
            selectedGameProperty.perform { game ->
                game?.let { gameDetailsView.game.valueFromView = it }
            }

            verticalGap(size = 30)

            // Bottom
            addComponent(browser)
        }
    }

    init {
        register()

        matchingGame.property.onChange { match ->
            if (match != null) {
                gamesTable.selectionModel.select(match)
            }
        }
    }

    private fun EventTarget.additionalInfoView() = tableview<Filter.Context.AdditionalData> {
        makeIndexColumn().apply { addClass(CommonStyle.centered) }
//        simpleColumn("Rule") { result -> result.rule.name }
        customGraphicColumn("Value") {
            val result = it.value
            when (result) {
                is Filter.NameDiff.GameNameFolderDiff -> DiffResultFragment(result, selectedGameProperty.value).root
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
                is Filter.NameDiff.GameNameFolderDiff -> providerLogos[result.providerId]!!.image.toImageView(height = 80.0, width = 160.0)
                is Filter.Duplications.GameDuplication -> providerLogos[result.providerId]!!.image.toImageView(height = 80.0, width = 160.0)
                else -> label()
            }
        }

        additionalInfoProperty.onChange { additionalInfo ->
            // The selected game may not appear in the report (can happen with programmatic selection).
            items = additionalInfo?.toList()?.observable()
            resizeColumnsToFitContent()
        }
    }

    private fun EventTarget.container(text: ObservableValue<String>, op: VBox.() -> Unit) = vbox {
        alignment = Pos.CENTER
        header(text)
        op(this)
    }

    override fun browseTo(url: String?) = browser.load(url)

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
            textProperty().bind(selectedGameProperty.stringBinding { "Exclude ${it?.name ?: ""}" })
            enableWhen { selectedGameProperty.isNotNull }
            action(excludeGameActions) { selectedGameProperty.value }
        }
    }
}