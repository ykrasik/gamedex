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

package com.gitlab.ykrasik.gamedex.ui.view.report

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.core.api.file.FileSystemService
import com.gitlab.ykrasik.gamedex.core.game.Filter
import com.gitlab.ykrasik.gamedex.core.matchesSearchQuery
import com.gitlab.ykrasik.gamedex.core.report.ReportConfig
import com.gitlab.ykrasik.gamedex.javafx.*
import com.gitlab.ykrasik.gamedex.javafx.game.GameController
import com.gitlab.ykrasik.gamedex.javafx.report.ReportController
import com.gitlab.ykrasik.gamedex.ui.view.game.details.GameDetailsFragment
import com.gitlab.ykrasik.gamedex.ui.view.game.details.YouTubeWebBrowser
import com.gitlab.ykrasik.gamedex.ui.view.game.menu.GameContextMenu
import javafx.beans.property.SimpleStringProperty
import javafx.beans.value.ObservableValue
import javafx.collections.ObservableList
import javafx.event.EventTarget
import javafx.geometry.Pos
import javafx.scene.control.TableView
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import tornadofx.*

/**
 * User: ykrasik
 * Date: 11/06/2017
 * Time: 09:48
 */
class ReportView(val reportConfig: ReportConfig) : View(reportConfig.name, Theme.Icon.chart()) {
    private val reportController: ReportController by di()
    private val gameController: GameController by di()
    private val fileSystemService: FileSystemService by di()

    private val gameContextMenu: GameContextMenu by inject()
    private val browser = YouTubeWebBrowser()

    val report = reportController.generateReport(reportConfig)
    private val games = report.resultsProperty.mapToList { it.keys.sortedBy { it.name } }

    val searchProperty = SimpleStringProperty("")

    private val gamesTable = gamesView(games)

    val selectedGameProperty = gamesTable.selectionModel.selectedItemProperty()
    private val selectedGame by selectedGameProperty

    private val additionalInfoProperty = selectedGameProperty.map { report.results[it] }

    override val root = hbox {
        useMaxSize = true

        // Left
        vbox {
            // Top
            container(games.mapProperty { "Games: ${it.size}" }) {
                vgrow = Priority.ALWAYS
                minHeight = screenBounds.height / 2
                children += gamesTable
            }

            // Bottom
            container("Additional Info".toProperty()) {
                showWhen { additionalInfoProperty.map { it?.isNotEmpty() ?: false } }
                additionalInfoView()
            }
        }

        verticalSeparator(padding = 4.0)

        // Right
        vbox {
            hgrow = Priority.ALWAYS

            // Top
            stackpane {
                paddingAll = 5.0
                selectedGameProperty.perform { game ->
                    if (game != null) {
                        replaceChildren {
                            children += GameDetailsFragment(game).root
                        }
                    }
                }
            }

            separator()

            // Bottom
            children += browser.root.apply { vgrow = Priority.ALWAYS }
            selectedGameProperty.perform { game ->
                if (game != null) browser.searchYoutube(game)
            }
        }
    }

    private fun gamesView(games: ObservableList<Game>) = TableView(games).apply {
        vgrow = Priority.ALWAYS
        fun isNotSelected(game: Game) = selectionModel.selectedItemProperty().isNotEqualTo(game)

        val indexColumn = makeIndexColumn().apply { addClass(CommonStyle.centered) }
        readonlyColumn("Game", Game::name)
        customGraphicColumn("Path") { game ->
            pathButton(game.path) { mouseTransparentWhen { isNotSelected(game) } }
        }
        customGraphicColumn("Size", { game -> fileSystemService.size(game.path).toObservableValue() }) { size ->
            label(size.humanReadable)
        }.apply { minWidth = 60.0 }

        gameContextMenu.install(this) { selectionModel.selectedItem }
        onUserSelect { gameController.viewDetails(it) }

        searchProperty.onChange { query ->
            if (query.isNullOrEmpty()) return@onChange
            val match = items.firstOrNull { it.matchesSearchQuery(query!!) }
            if (match != null) {
                selectionModel.select(match)
                scrollTo(match)
            }
        }

        report.resultsProperty.onChange { resizeColumnsToFitContent() }

        minWidthFitContent(indexColumn)
    }

    private fun EventTarget.additionalInfoView() = tableview<Filter.AdditionalData> {
        makeIndexColumn().apply { addClass(CommonStyle.centered) }
//        simpleColumn("Rule") { result -> result.rule.name }
        customGraphicColumn("Value") {
            val result = it.value
            when (result) {
                is Filter.NameDiff.GameNameFolderDiff -> DiffResultFragment(result, selectedGame).root
                is Filter.Duplications.GameDuplication -> DuplicationFragment(result, gamesTable).root
                else -> label(result.toDisplayString())
            }
        }
        customGraphicColumn("Extra") {
            val result = it.value
            when (result) {
                is Filter.NameDiff.GameNameFolderDiff -> ProviderLogoFragment(result.providerId).root
                is Filter.Duplications.GameDuplication -> ProviderLogoFragment(result.providerId).root
                else -> label()
            }
        }

        additionalInfoProperty.onChange { additionalInfo ->
            // The selected game may not appear in the report (can happen with programmatic selection).
            items = additionalInfo?.observable()
            resizeColumnsToFitContent()
        }
    }

    private fun EventTarget.container(text: ObservableValue<String>, op: VBox.() -> Unit) = vbox {
        alignment = Pos.CENTER
        header(text)
        op(this)
    }

    override fun onDock() {
        browser.stop()
        report.start()
    }

    override fun onUndock() {
        browser.stop()
        report.stop()
    }
}