package com.gitlab.ykrasik.gamedex.ui.view.report

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.controller.GameController
import com.gitlab.ykrasik.gamedex.controller.ReportsController
import com.gitlab.ykrasik.gamedex.core.GameNameFolderMismatch
import com.gitlab.ykrasik.gamedex.core.GameNameFolderMismatches
import com.gitlab.ykrasik.gamedex.repository.GameProviderRepository
import com.gitlab.ykrasik.gamedex.ui.*
import com.gitlab.ykrasik.gamedex.ui.theme.CommonStyle
import com.gitlab.ykrasik.gamedex.ui.theme.Theme
import com.gitlab.ykrasik.gamedex.ui.theme.pathButton
import com.gitlab.ykrasik.gamedex.ui.view.game.menu.GameContextMenu
import javafx.geometry.Pos
import javafx.scene.control.TableView
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.scene.text.FontWeight
import tornadofx.*

/**
 * User: ykrasik
 * Date: 10/06/2017
 * Time: 18:41
 */
class NameFolderMismatchView : ReportView<GameNameFolderMismatches>("Name-Folder Mismatch", Theme.Icon.report()) {
    private val providerRepository: GameProviderRepository by di()
    private val gameController: GameController by di()
    private val reportsController: ReportsController by di()

    private val gameContextMenu: GameContextMenu by inject()

    private var mainTable: TableView<Game> by singleAssign()

    override val ongoingReport = reportsController.nameFolderMismatches
    private val mismatches = ongoingReport.resultsProperty

    override val root = run {
        val left = mainTable()
        val right = sideTable()
        splitpane(left, right) { setDividerPositions(0.5) }
    }

    private fun mainTable() = container("Source") {
        mainTable = tableview(mismatches.mapToList { it.keys.sortedBy { it.name } }) {
            vgrow = Priority.ALWAYS
            fun isNotSelected(game: Game) = selectionModel.selectedItemProperty().isNotEqualTo(game)

            makeIndexColumn().apply { addClass(CommonStyle.centered) }
            column("Name", Game::name)
            customGraphicColumn("Path") { game ->
                pathButton(game.path) { mouseTransparentWhen { isNotSelected(game) } }
            }

            gameContextMenu.install(this) { selectionModel.selectedItem }
            onUserSelect { gameController.viewDetails(it) }

            mismatches.onChange { resizeColumnsToFitContent() }
        }
    }

    private fun sideTable() = container("Mismatches") {
        tableview<GameNameFolderMismatch> {
            vgrow = Priority.ALWAYS
            allowDeselection(onClickAgain = true)

            makeIndexColumn().apply { addClass(CommonStyle.centered) }
            imageViewColumn("Provider", fitHeight = 80.0, fitWidth = 160.0, isPreserveRatio = true) { mismatch ->
                providerRepository.logo(mismatch.providerId).toProperty()
            }
            customGraphicColumn("Expected Name") { mismatch -> label(mismatch.expectedName) }

            mainTable.selectionModel.selectedItemProperty().onChange { selectedGame ->
                items = selectedGame?.let { mismatches.value[it]!!.observable() }
                resizeColumnsToFitContent()
            }
        }
    }

    private fun container(text: String, op: VBox.() -> Unit) = vbox {
        alignment = Pos.CENTER
        label(text) { addClass(Style.headerLabel) }
        op(this)
    }

    class Style : Stylesheet() {
        companion object {
            val headerLabel by cssclass()

            init {
                importStylesheet(Style::class)
            }
        }

        init {
            headerLabel {
                fontSize = 18.px
                fontWeight = FontWeight.BOLD
            }
        }
    }
}