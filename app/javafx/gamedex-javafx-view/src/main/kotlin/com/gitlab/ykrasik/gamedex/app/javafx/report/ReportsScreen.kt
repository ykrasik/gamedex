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

package com.gitlab.ykrasik.gamedex.app.javafx.report

import com.gitlab.ykrasik.gamedex.FileStructure
import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.app.api.file.ViewCanBrowseFile
import com.gitlab.ykrasik.gamedex.app.api.filter.Filter
import com.gitlab.ykrasik.gamedex.app.api.game.ViewCanShowGameDetails
import com.gitlab.ykrasik.gamedex.app.api.image.Image
import com.gitlab.ykrasik.gamedex.app.api.image.ViewWithProviderLogos
import com.gitlab.ykrasik.gamedex.app.api.report.*
import com.gitlab.ykrasik.gamedex.app.api.util.channel
import com.gitlab.ykrasik.gamedex.app.api.web.ViewWithBrowser
import com.gitlab.ykrasik.gamedex.app.javafx.game.GameContextMenu
import com.gitlab.ykrasik.gamedex.app.javafx.game.details.JavaFxGameDetailsView
import com.gitlab.ykrasik.gamedex.app.javafx.image.image
import com.gitlab.ykrasik.gamedex.javafx.*
import com.gitlab.ykrasik.gamedex.javafx.dialog.areYouSureDialog
import com.gitlab.ykrasik.gamedex.javafx.view.PresentableScreen
import com.gitlab.ykrasik.gamedex.javafx.view.WebBrowser
import com.gitlab.ykrasik.gamedex.provider.ProviderId
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.value.ObservableValue
import javafx.event.EventTarget
import javafx.geometry.Pos
import javafx.scene.control.Toggle
import javafx.scene.control.ToggleGroup
import javafx.scene.control.ToolBar
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import tornadofx.*
import java.io.File

/**
 * User: ykrasik
 * Date: 10/06/2017
 * Time: 16:25
 */
class ReportsScreen : PresentableScreen("Reports", Theme.Icon.chart()),
    ViewWithReports,
    ReportView,
    ViewCanAddReport,
    ViewCanEditReport,
    ViewCanExcludeGameFromReport,
    ViewCanDeleteReport,
    ViewCanSearchReports,
    ViewCanShowGameDetails,
    ViewWithProviderLogos,
    ViewWithBrowser,
    ViewCanBrowseFile {

    override val reports = mutableListOf<Report>().observable()
    override val addReportActions = channel<Unit>()
    override val editReportActions = channel<Report>()
    override val excludeGameActions = channel<Pair<Report, Game>>()
    override val deleteReportActions = channel<Report>()

    private val gameContextMenu: GameContextMenu by inject()
    private val browser = WebBrowser()

    override var providerLogos = emptyMap<ProviderId, Image>()

    override val reportChanges = channel<Report?>()
    private val reportProperty = SimpleObjectProperty<Report>().eventOnChange(reportChanges)
    override var report by reportProperty

    private val resultProperty = SimpleObjectProperty<ReportResult>(ReportResult(emptyList(), emptyMap(), emptyMap()))
    override var result by resultProperty

    private val games = resultProperty.mapToList { it.games }

    override val showGameDetailsActions = channel<Game>()

    override val searchTextChanges = channel<String>()
    private val searchTextProperty = SimpleStringProperty("").eventOnChange(searchTextChanges)
    override var searchText by searchTextProperty

    private val matchingGameProperty = SimpleObjectProperty<Game?>(null)
    override var matchingGame by matchingGameProperty

    private val gamesTable = tableview(games) {
        vgrow = Priority.ALWAYS
        fun isNotSelected(game: Game) = selectionModel.selectedItemProperty().isNotEqualTo(game)

        val indexColumn = makeIndexColumn().apply { addClass(CommonStyle.centered) }
        readonlyColumn("Game", Game::name)
        customGraphicColumn("Path") { game ->
            jfxButton(game.path.toString()) {
                mouseTransparentWhen { isNotSelected(game) }
                isFocusTraversable = false
                eventOnAction(browseToFileActions) { game.path }
            }
        }
        customGraphicColumn("Size", { game -> (result.fileStructure[game.id] ?: FileStructure.NotAvailable).size.toProperty() }) { size ->
            label(size.humanReadable)
        }.apply { minWidth = 60.0 }

        gameContextMenu.install(this) { selectionModel.selectedItem }
        onUserSelect { showGameDetailsActions.event(it) }

        resultProperty.onChange { resizeColumnsToFitContent() }

        minWidthFitContent(indexColumn)
    }

    override val gameChanges = channel<Game?>()
    private val selectedGameProperty = gamesTable.selectionModel.selectedItemProperty().eventOnNullableChange(gameChanges)
    override val game by selectedGameProperty

    private val additionalInfoProperty = selectedGameProperty.map { result.additionalData[it?.id] }

    override val browseToFileActions = channel<File>()

    private val gameDetailsView = JavaFxGameDetailsView()

    private val selection = ToggleGroup()
    private val currentToggle get() = selection.selectedToggleProperty()
    private val currentReport = currentToggle.map { it?.report }

    private var isRenderingReports = false
    private var ignoreNextToggleChange = false

    override val root = hbox {
        vgrow = Priority.ALWAYS
        useMaxSize = true
        visibleWhen { currentReport.isNotNull }

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
            addComponent(gameDetailsView) {
                root.paddingAll = 5.0
            }
            selectedGameProperty.perform { game ->
                game?.let { gameDetailsView.game = it }
            }

            separator()

            // Bottom
            addComponent(browser)
        }
    }

    init {
        viewRegistry.onCreate(this)

        matchingGameProperty.onChange { match ->
            if (match != null) {
                gamesTable.selectionModel.select(match)
                gamesTable.scrollTo(match)
            }
        }

        selection.selectedToggleProperty().addListener { _, oldToggle, newToggle ->
            if (oldToggle != null && newToggle == null) {
                // This piece of code prevents a toggle from being 'unselected', as we always want one active.
                // It will get called also when we are re-constructing the screens after a change to the settings,
                // where it must allow the old toggle to be unselected (it is about to be dropped completely).
                if (!isRenderingReports) {
                    ignoreNextToggleChange = true
                    selection.selectToggle(oldToggle)
                    ignoreNextToggleChange = false
                }
            } else {
                if (!ignoreNextToggleChange) {
                    report = newToggle.report
                }
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
                is Filter.Duplications.GameDuplication -> DuplicationFragment(result, gamesTable.items, matchingGameProperty).root
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

    override fun ToolBar.constructToolbar() {
        buttonWithPopover("None", graphic = Theme.Icon.chart()) {
            reports.perform { reports ->
                val prevReport = currentReport.value

                isRenderingReports = true
                selection.toggles.clear()

                replaceChildren {
                    gridpane {
                        hgap = 5.0
                        reports.forEach { report ->
                            row {
                                jfxToggleNode(report.name, Theme.Icon.chart(), group = selection) {
                                    useMaxWidth = true
                                    userData = report
                                    isSelected = this == currentToggle.value
                                }
                                jfxButton(graphic = Theme.Icon.edit()) {
                                    eventOnAction(editReportActions) { report }
                                }
                                jfxButton(graphic = Theme.Icon.delete()) {
                                    addClass(CommonStyle.deleteButton)
                                    eventOnAction(deleteReportActions) { report }
                                }
                            }
                        }
                    }
                    separator()
                    addButton {
                        useMaxWidth = true
                        alignment = Pos.CENTER
                        addClass(CommonStyle.thinBorder)
                        eventOnAction(addReportActions)
                    }
                }

                isRenderingReports = false

                if (prevReport != null) {
                    // Re-select the previous report if it wasn't deleted.
                    selection.selectToggle(selection.toggles.find { it.report.id == prevReport.id })
                }
            }
        }.apply {
            addClass(CommonStyle.thinBorder)
            alignment = Pos.CENTER_LEFT
            textProperty().bind(currentReport.map { it?.name ?: "Select Report" })
        }
        verticalSeparator()
        searchField(this@ReportsScreen, searchTextProperty) { isFocusTraversable = false }
        verticalSeparator()
        spacer()
        verticalSeparator()
        excludeButton {
            textProperty().bind(selectedGameProperty.map { if (it != null) "Exclude ${it.name}" else "Exclude" })
            enableWhen { selectedGameProperty.isNotNull }
            eventOnAction(excludeGameActions) { currentReport.value!! to selectedGameProperty.value }
        }
        verticalSeparator()
    }

    override fun confirmDelete(report: Report) =
    // TODO: Display a read-only view of the rules instead.
        areYouSureDialog("Delete report '${report.name}'?") { label("Rules: ${report.filter}") }

    private val Toggle.report get() = userData as Report
}