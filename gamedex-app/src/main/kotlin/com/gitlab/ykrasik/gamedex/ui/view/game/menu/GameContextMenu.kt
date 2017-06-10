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
import javafx.scene.Node
import javafx.scene.input.MouseEvent
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
        jfxButton("View", Theme.Icon.view(20.0)) {
            addClass(CommonStyle.fillAvailableWidth)
            setOnAction { controller.viewDetails(game) }
        }
        separator()
        jfxButton("Edit", Theme.Icon.edit(20.0)) {
            addClass(CommonStyle.fillAvailableWidth)
            setOnAction { controller.editDetails(game) }
        }
        jfxButton("Change Thumbnail", Theme.Icon.thumbnail(20.0)) {
            addClass(CommonStyle.fillAvailableWidth)
            setOnAction { controller.editDetails(game, initialTab = GameDataType.thumbnail) }
        }
        separator()
        jfxButton("Tag", Theme.Icon.tag(20.0)) {
            addClass(CommonStyle.fillAvailableWidth)
            setOnAction { controller.tag(game) }
        }
        separator()
        jfxButton("Refresh", Theme.Icon.refresh(20.0)) {
            addClass(CommonStyle.fillAvailableWidth)
            enableWhen { controller.canRunLongTask }
            setOnAction { controller.refreshGame(game) }
        }
        jfxButton("Search", Theme.Icon.search(20.0)) {
            addClass(CommonStyle.fillAvailableWidth)
            enableWhen { controller.canRunLongTask }
            dropDownMenu(PopOver.ArrowLocation.LEFT_TOP, closeOnClick = false) {
                ChooseSearchResultsToggleMenu().install(this)
            }
            setOnAction { controller.searchGame(game) }
        }
        separator()
        jfxButton("Delete", Theme.Icon.delete(20.0)) {
            addClass(CommonStyle.fillAvailableWidth)
            setOnAction { controller.delete(game) }
        }
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