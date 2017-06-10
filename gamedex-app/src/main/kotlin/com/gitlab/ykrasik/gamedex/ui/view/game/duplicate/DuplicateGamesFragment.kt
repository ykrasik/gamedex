package com.gitlab.ykrasik.gamedex.ui.view.game.duplicate

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.controller.GameController
import com.gitlab.ykrasik.gamedex.repository.GameProviderRepository
import com.gitlab.ykrasik.gamedex.task.GameTasks
import com.gitlab.ykrasik.gamedex.ui.*
import com.gitlab.ykrasik.gamedex.ui.theme.CommonStyle
import com.gitlab.ykrasik.gamedex.ui.theme.acceptButton
import com.gitlab.ykrasik.gamedex.ui.theme.gameContextMenu
import com.gitlab.ykrasik.gamedex.ui.theme.pathButton
import javafx.event.EventTarget
import javafx.scene.control.TableView
import javafx.stage.Screen
import tornadofx.*

/**
 * User: ykrasik
 * Date: 10/06/2017
 * Time: 11:32
 */
class DuplicateGamesFragment(private val games: Map<Game, List<GameTasks.GameDuplication>>) : Fragment("Duplicate Games") {
    private val providerRepository: GameProviderRepository by di()
    private val gameController: GameController by di()

    private var mainTable: TableView<Game> by singleAssign()
    private var sideTable: TableView<GameTasks.GameDuplication> by singleAssign()

    override val root = borderpane {
        with(Screen.getPrimary().bounds) {
            prefHeight = height * 3 / 4
            prefWidth = width * 2 / 3
        }
        top {
            acceptButton { setOnAction { close() } }
        }
        center {
            mainTable = mainTable()
            sideTable = sideTable()
            splitpane(mainTable, sideTable) { setDividerPositions(0.5) }
        }
    }

    private fun EventTarget.mainTable() = tableview(games.keys.sortedBy { it.name }.observable()) {
        fun isNotSelected(game: Game) = selectionModel.selectedItemProperty().isNotEqualTo(game)

        makeIndexColumn().apply { addClass(CommonStyle.centered) }
        column("Name", Game::name)
        customGraphicColumn("Path") { game ->
            pathButton(game.path) { mouseTransparentWhen { isNotSelected(game) } }
        }

        gameContextMenu(gameController) { selectionModel.selectedItem }
        onUserSelect { gameController.viewDetails(it) }
    }

    private fun EventTarget.sideTable() = tableview<GameTasks.GameDuplication> {
        allowDeselection(onClickAgain = true)

        makeIndexColumn().apply { addClass(CommonStyle.centered) }
        imageViewColumn("Provider", fitHeight = 80.0, fitWidth = 160.0, isPreserveRatio = true) { (_, providerHeader) ->
            providerRepository.logo(providerHeader.id).toProperty()
        }
        customGraphicColumn("Name") { (game, _) ->
            jfxButton(game.name) { setOnAction { mainTable.selectionModel.select(game) } }
        }
        customGraphicColumn("Path") { pathButton(it.game.path) }

        mainTable.selectionModel.selectedItemProperty().onChange {
            items = games[it!!]!!.observable()
            resizeColumnsToFitContent()
        }
    }

    override fun onDock() {
        modalStage!!.isMaximized = true
    }

    fun show() {
        openWindow(block = true, owner = null)
    }
}