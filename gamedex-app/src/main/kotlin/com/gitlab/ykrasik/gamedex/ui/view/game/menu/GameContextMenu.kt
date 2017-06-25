package com.gitlab.ykrasik.gamedex.ui.view.game.menu

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.GameDataType
import com.gitlab.ykrasik.gamedex.controller.GameController
import com.gitlab.ykrasik.gamedex.ui.dropDownMenu
import com.gitlab.ykrasik.gamedex.ui.jfxButton
import com.gitlab.ykrasik.gamedex.ui.popOver
import com.gitlab.ykrasik.gamedex.ui.replaceContent
import com.gitlab.ykrasik.gamedex.ui.theme.CommonStyle
import com.gitlab.ykrasik.gamedex.ui.theme.Theme
import com.jfoenix.controls.JFXButton
import javafx.scene.Node
import javafx.scene.input.MouseEvent
import javafx.scene.layout.VBox
import org.controlsfx.control.PopOver
import tornadofx.*

/**
 * User: ykrasik
 * Date: 10/06/2017
 * Time: 21:20
 */
class GameContextMenu : View() {
    private val controller: GameController by di()

    private lateinit var game: Game

    override val root = vbox {
        addClass(CommonStyle.popoverMenu)
        val size = 20.0
        item("View", Theme.Icon.view(size)) { setOnAction { controller.viewDetails(game) } }
        separator()
        item("Edit", Theme.Icon.edit(size)) { setOnAction { controller.editDetails(game) } }
        item("Change Thumbnail", Theme.Icon.thumbnail(size)) {
            setOnAction { controller.editDetails(game, initialTab = GameDataType.thumbnail) }
        }
        separator()
        item("Tag", Theme.Icon.tag(size)) { setOnAction { controller.tag(game) } }
        separator()
        item("Refresh", Theme.Icon.refresh(size)) {
            enableWhen { controller.canRunLongTask }
            setOnAction { controller.refreshGame(game) }
        }
        item("Search", Theme.Icon.search(size)) {
            enableWhen { controller.canRunLongTask }
            dropDownMenu(PopOver.ArrowLocation.LEFT_TOP, closeOnClick = false) {
                ChooseSearchResultsToggleMenu().install(this)
            }
            setOnAction { controller.searchGame(game) }
        }
        separator()
        item("Rename/Move Folder", Theme.Icon.folder(size)) { setOnAction { controller.renameFolder(game) }}
        separator()
        item("Delete", Theme.Icon.delete(size)) { setOnAction { controller.delete(game) } }
    }

    private fun VBox.item(text: String, icon: Node, op: JFXButton.() -> Unit) = jfxButton(text, icon, op = op).apply {
        addClass(CommonStyle.fillAvailableWidth)
    }

    private val popover = popOver { children += root }.apply { isAutoFix = false }

    fun install(node: Node, game: () -> Game) {
        node.addEventHandler(MouseEvent.MOUSE_CLICKED) { popover.hide() }
        node.setOnContextMenuRequested { e ->
            this.game = game()
            popover.replaceContent(root)
            popover.show(node, e.screenX, e.screenY)
        }
    }
}