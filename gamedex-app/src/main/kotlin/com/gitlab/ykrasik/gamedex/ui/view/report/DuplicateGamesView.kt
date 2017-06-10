package com.gitlab.ykrasik.gamedex.ui.view.report

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.controller.GameController
import com.gitlab.ykrasik.gamedex.controller.ReportsController
import com.gitlab.ykrasik.gamedex.core.GameDuplication
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
 * Time: 11:32
 */
class DuplicateGamesView : View("Duplicate Games", Theme.Icon.report()) {
    private val providerRepository: GameProviderRepository by di()
    private val gameController: GameController by di()
    private val reportsController: ReportsController by di()

    private val gameContextMenu: GameContextMenu by inject()

    private var mainTable: TableView<Game> by singleAssign()

    override val root = stackpane {
        val left = mainTable()
        val right = sideTable()
        splitpane(left, right) { setDividerPositions(0.5) }
        maskerPane { visibleWhen { reportsController.calculatingDuplicationsProperty } }
    }

    private fun mainTable() = container("Source") {
        mainTable = tableview(reportsController.gameDuplications.mapToList { it.keys.sortedBy { it.name } }) {
            vgrow = Priority.ALWAYS
            fun isNotSelected(game: Game) = selectionModel.selectedItemProperty().isNotEqualTo(game)

            makeIndexColumn().apply { addClass(CommonStyle.centered) }
            column("Name", Game::name)
            customGraphicColumn("Path") { game ->
                pathButton(game.path) { mouseTransparentWhen { isNotSelected(game) } }
            }

            gameContextMenu.install(this) { selectionModel.selectedItem }
            onUserSelect { gameController.viewDetails(it) }

            reportsController.gameDuplications.onChange {
                resizeColumnsToFitContent()
            }
        }
    }

    private fun sideTable() = container("Duplications") {
        tableview<GameDuplication> {
            vgrow = Priority.ALWAYS
            allowDeselection(onClickAgain = true)

            makeIndexColumn().apply { addClass(CommonStyle.centered) }
            imageViewColumn("Provider", fitHeight = 80.0, fitWidth = 160.0, isPreserveRatio = true) { (_, providerHeader) ->
                providerRepository.logo(providerHeader.id).toProperty()
            }
            customGraphicColumn("Name") { (game, _) ->
                jfxButton(game.name) {
                    setOnAction {
                        mainTable.selectionModel.select(game)
                        mainTable.scrollTo(game)
                    }
                }
            }
            customGraphicColumn("Path") { pathButton(it.first.path) }

            mainTable.selectionModel.selectedItemProperty().onChange { selectedGame ->
                items = selectedGame?.let { reportsController.gameDuplications.value[it]!!.observable() }
                resizeColumnsToFitContent()
            }
        }
    }

    private fun container(text: String, op: VBox.() -> Unit) = vbox {
        alignment = Pos.CENTER
        label(text) { addClass(Style.headerLabel) }
        op(this)
    }

    override fun onDock() {
        // This is called on application startup, but we don't want to start any calculations before the user explicitly entered the reports screen.
        // A bit of a hack.
        skipFirstTime {
            reportsController.startDetectingDuplications()
        }
    }

    override fun onUndock() {
        reportsController.stopDetectingDuplications()
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