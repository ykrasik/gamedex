package com.gitlab.ykrasik.gamedex.ui.view.report

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.controller.GameController
import com.gitlab.ykrasik.gamedex.controller.ReportsController
import com.gitlab.ykrasik.gamedex.ui.customGraphicColumn
import com.gitlab.ykrasik.gamedex.ui.mapToList
import com.gitlab.ykrasik.gamedex.ui.mouseTransparentWhen
import com.gitlab.ykrasik.gamedex.ui.skipFirstTime
import com.gitlab.ykrasik.gamedex.ui.theme.CommonStyle
import com.gitlab.ykrasik.gamedex.ui.theme.pathButton
import com.gitlab.ykrasik.gamedex.ui.view.game.menu.GameContextMenu
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.TableView
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.scene.text.FontWeight
import tornadofx.*

/**
 * User: ykrasik
 * Date: 11/06/2017
 * Time: 09:48
 */
abstract class ReportView<T>(title: String, icon: Node) : View(title, icon) {
    private val gameContextMenu: GameContextMenu by inject()

    protected val reportsController: ReportsController by di()
    protected val gameController: GameController by di()

    private var gamesTable: TableView<Game> by singleAssign()
    protected var reportsView: Node by singleAssign()

    abstract val ongoingReport: ReportsController.OngoingReport<T>
    protected abstract val reportHeader: String
    protected abstract fun reportsView(): Node
    open val extraOptions: VBox? = null

    override val root = run {
        val left = container("Games") {
            gamesTable = gamesView()
            children += gamesTable
        }
        val right = container(reportHeader) {
            reportsView = reportsView().apply { vgrow = Priority.ALWAYS }
            children += reportsView
        }
        splitpane(left, right) { setDividerPositions(0.5) }
    }

    private fun gamesView() = tableview(ongoingReport.resultsProperty.mapToList { it.keys.sortedBy { it.name } }) {
        vgrow = Priority.ALWAYS
        fun isNotSelected(game: Game) = selectionModel.selectedItemProperty().isNotEqualTo(game)

        makeIndexColumn().apply { addClass(CommonStyle.centered) }
        column("Game", Game::name)
        customGraphicColumn("Path") { game ->
            pathButton(game.path) { mouseTransparentWhen { isNotSelected(game) } }
        }

        gameContextMenu.install(this) { selectionModel.selectedItem }
        onUserSelect { gameController.viewDetails(it) }

        ongoingReport.resultsProperty.onChange { resizeColumnsToFitContent() }
    }

    protected val selectedGameProperty get() = gamesTable.selectionModel.selectedItemProperty()
    protected val selectedGame get() = gamesTable.selectionModel.selectedItem
    protected fun selectGame(game: Game) = gamesTable.selectionModel.select(game)
    protected fun scrollTo(game: Game) = gamesTable.scrollTo(game)

    private fun container(text: String, op: VBox.() -> Unit) = vbox {
        alignment = Pos.CENTER
        label(text) { addClass(Style.headerLabel) }
        op(this)
    }

    override fun onDock() {
        // This is called on application startup, but we don't want to start any calculations before
        // the user explicitly entered the reports screen. A bit of a hack.
        skipFirstTime { ongoingReport.start() }
    }

    override fun onUndock() = ongoingReport.stop()

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