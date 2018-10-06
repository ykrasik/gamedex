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

package com.gitlab.ykrasik.gamedex.javafx.report

import com.gitlab.ykrasik.gamedex.FileStructure
import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.app.api.filter.Filter
import com.gitlab.ykrasik.gamedex.app.api.game.ViewCanShowGameDetails
import com.gitlab.ykrasik.gamedex.app.api.image.Image
import com.gitlab.ykrasik.gamedex.app.api.image.ViewWithProviderLogos
import com.gitlab.ykrasik.gamedex.app.api.report.Report
import com.gitlab.ykrasik.gamedex.app.api.report.ReportResult
import com.gitlab.ykrasik.gamedex.app.api.report.ReportView
import com.gitlab.ykrasik.gamedex.app.api.util.channel
import com.gitlab.ykrasik.gamedex.app.javafx.game.GameContextMenu
import com.gitlab.ykrasik.gamedex.app.javafx.image.image
import com.gitlab.ykrasik.gamedex.core.game.matchesSearchQuery
import com.gitlab.ykrasik.gamedex.javafx.*
import com.gitlab.ykrasik.gamedex.javafx.game.details.GameDetailsFragment
import com.gitlab.ykrasik.gamedex.javafx.view.PresentableView
import com.gitlab.ykrasik.gamedex.javafx.view.YouTubeWebBrowser
import com.gitlab.ykrasik.gamedex.provider.ProviderId
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.value.ObservableValue
import javafx.event.EventTarget
import javafx.geometry.Pos
import javafx.scene.control.ProgressIndicator
import javafx.scene.control.TableView
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import tornadofx.*

/**
 * User: ykrasik
 * Date: 11/06/2017
 * Time: 09:48
 */
class JavaFxReportView(override val report: Report) : PresentableView(report.name, Theme.Icon.chart()),
    ReportView, ViewCanShowGameDetails, ViewWithProviderLogos {
    override var providerLogos = emptyMap<ProviderId, Image>()

    val calculatingReportProperty = SimpleBooleanProperty(false)
    override var calculatingReport by calculatingReportProperty

    val calculatingReportProgressProperty = SimpleDoubleProperty(ProgressIndicator.INDETERMINATE_PROGRESS)
    override var calculatingReportProgress by calculatingReportProgressProperty

    private val resultProperty = SimpleObjectProperty<ReportResult>(ReportResult(emptyList(), emptyMap(), emptyMap()))
    override var result by resultProperty

    override val showGameDetailsActions = channel<Game>()

    private val gameContextMenu: GameContextMenu by inject()
    private val browser = YouTubeWebBrowser()

    private val games = resultProperty.mapToList { it.games }

    // TODO: ViewCanSearchReport
    val searchProperty = SimpleStringProperty("")

    private val gamesTable = gamesView()

    val selectedGameProperty = gamesTable.selectionModel.selectedItemProperty()
    private val additionalInfoProperty = selectedGameProperty.map { result.additionalData[it?.id] }

    init {
        viewRegistry.register(this)
    }

    override val root = hbox {
        useMaxSize = true

        // Left
        vbox {
            // Top
            container(games.sizeProperty.stringBinding { "Games: $it" }) {
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

    private fun gamesView() = TableView(games).apply {
        vgrow = Priority.ALWAYS
        fun isNotSelected(game: Game) = selectionModel.selectedItemProperty().isNotEqualTo(game)

        val indexColumn = makeIndexColumn().apply { addClass(CommonStyle.centered) }
        readonlyColumn("Game", Game::name)
        customGraphicColumn("Path") { game ->
            pathButton(game.path) { mouseTransparentWhen { isNotSelected(game) } }
        }
        customGraphicColumn("Size", { game -> (result.fileStructure[game.id] ?: FileStructure.NotAvailable).size.toProperty() }) { size ->
            label(size.humanReadable)
        }.apply { minWidth = 60.0 }

        gameContextMenu.install(this) { selectionModel.selectedItem }
        onUserSelect { showGameDetailsActions.event(it) }

        searchProperty.onChange { query ->
            if (query.isNullOrEmpty()) return@onChange
            val match = items.firstOrNull { it.matchesSearchQuery(query!!) }
            if (match != null) {
                selectionModel.select(match)
                scrollTo(match)
            }
        }

        resultProperty.onChange { resizeColumnsToFitContent() }

        minWidthFitContent(indexColumn)
    }

    private fun EventTarget.additionalInfoView() = tableview<Filter.Context.AdditionalData> {
        makeIndexColumn().apply { addClass(CommonStyle.centered) }
//        simpleColumn("Rule") { result -> result.rule.name }
        customGraphicColumn("Value") {
            val result = it.value
            when (result) {
                is Filter.NameDiff.GameNameFolderDiff -> DiffResultFragment(result, selectedGameProperty.value).root
                is Filter.Duplications.GameDuplication -> DuplicationFragment(result, gamesTable).root
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

    override fun onDock() {
        browser.stop()
    }

    override fun onUndock() {
        browser.stop()
    }
}