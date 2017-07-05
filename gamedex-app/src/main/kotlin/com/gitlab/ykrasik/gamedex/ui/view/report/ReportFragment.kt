package com.gitlab.ykrasik.gamedex.ui.view.report

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.controller.GameController
import com.gitlab.ykrasik.gamedex.controller.ReportsController
import com.gitlab.ykrasik.gamedex.core.FileSystemOps
import com.gitlab.ykrasik.gamedex.core.ReportConfig
import com.gitlab.ykrasik.gamedex.core.ReportRule
import com.gitlab.ykrasik.gamedex.core.matchesSearchQuery
import com.gitlab.ykrasik.gamedex.ui.*
import com.gitlab.ykrasik.gamedex.ui.theme.*
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
// TODO: Should consider making this a view and just re-binding the report to it.
class ReportFragment(val reportConfig: ReportConfig) : View(reportConfig.name, Theme.Icon.chart()) {
    private val reportsController: ReportsController by di()
    private val gameController: GameController by di()
    private val fileSystemOps: FileSystemOps by di()

    private val gameContextMenu: GameContextMenu by inject()
    private val browser = YouTubeWebBrowser()

    val report = reportsController.generateReport(reportConfig)
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
        column("Game", Game::name)
        customGraphicColumn("Path") { game ->
            pathButton(game.path) { mouseTransparentWhen { isNotSelected(game) } }
        }
        customGraphicColumn("Size", { game -> fileSystemOps.size(game.path) }) { size ->
            label(size.humanReadable)
        }.apply { minWidth = 60.0 }

        gameContextMenu.install(this) { selectionModel.selectedItem }
        onUserSelect { gameController.viewDetails(it) }

        searchProperty.onChange { query ->
            if (query.isNullOrEmpty()) return@onChange
            val match = items.filter { it.matchesSearchQuery(query!!) }.firstOrNull()
            if (match != null) {
                selectionModel.select(match)
                scrollTo(match)
            }
        }

        report.resultsProperty.onChange { resizeColumnsToFitContent() }

        minWidthFitContent(indexColumn)
    }

    private fun EventTarget.additionalInfoView() = tableview<ReportRule.ReportInfo> {
        makeIndexColumn().apply { addClass(CommonStyle.centered) }
        simpleColumn("Rule") { result -> result.ruleName }
        customGraphicColumn("Value") { result ->
            when (result.value) {
                is ReportRule.Rules.GameNameFolderDiff -> DiffResultFragment(result.value, selectedGame).root
                is ReportRule.Rules.GameDuplication -> DuplicationFragment(result.value, gamesTable).root
                else -> label(result.value.toDisplayString())
            }
        }
        customGraphicColumn("Extra") { result ->
            when (result.value) {
                is ReportRule.Rules.GameNameFolderDiff -> ProviderLogoFragment(result.value.providerId).root
                is ReportRule.Rules.GameDuplication -> ProviderLogoFragment(result.value.providerId).root
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