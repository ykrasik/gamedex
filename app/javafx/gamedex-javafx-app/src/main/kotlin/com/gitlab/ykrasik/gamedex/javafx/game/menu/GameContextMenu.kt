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

package com.gitlab.ykrasik.gamedex.javafx.game.menu

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.GameDataType
import com.gitlab.ykrasik.gamedex.app.api.game.common.ViewCanDeleteGame
import com.gitlab.ykrasik.gamedex.app.api.game.discover.ViewCanRediscoverGame
import com.gitlab.ykrasik.gamedex.app.api.game.download.ViewCanRedownloadGame
import com.gitlab.ykrasik.gamedex.app.api.game.edit.ViewCanEditGame
import com.gitlab.ykrasik.gamedex.app.api.game.rename.ViewCanRenameMoveGame
import com.gitlab.ykrasik.gamedex.app.api.game.tag.ViewCanTagGame
import com.gitlab.ykrasik.gamedex.app.api.presenters
import com.gitlab.ykrasik.gamedex.app.javafx.game.discover.discoverGameChooseResultsMenu
import com.gitlab.ykrasik.gamedex.app.javafx.game.edit.JavaFxEditGameView
import com.gitlab.ykrasik.gamedex.app.javafx.game.tag.JavaFxTagGameView
import com.gitlab.ykrasik.gamedex.javafx.*
import com.gitlab.ykrasik.gamedex.javafx.game.GameController
import com.gitlab.ykrasik.gamedex.javafx.game.common.DeleteGameView
import com.gitlab.ykrasik.gamedex.javafx.game.rename.JavaFxRenameMoveGameView
import com.gitlab.ykrasik.gamedex.javafx.screen.PresentableView
import com.jfoenix.controls.JFXButton
import javafx.scene.Node
import javafx.scene.input.MouseEvent
import javafx.scene.layout.VBox
import org.controlsfx.control.PopOver
import tornadofx.addClass
import tornadofx.enableWhen
import tornadofx.separator
import tornadofx.vbox

/**
 * User: ykrasik
 * Date: 10/06/2017
 * Time: 21:20
 */
class GameContextMenu : PresentableView(),
    ViewCanTagGame, ViewCanDeleteGame, ViewCanRedownloadGame, ViewCanRediscoverGame, ViewCanEditGame, ViewCanRenameMoveGame {
    private val tagGameView: JavaFxTagGameView by inject()
    private val editGameView: JavaFxEditGameView by inject()
    private val renameMoveGameView: JavaFxRenameMoveGameView by inject()

    private val controller: GameController by di()

    private val tagGamePresenter = presenters.tagGame.present(this)
    private val deleteGamePresenter = presenters.deleteGame.present(this)
    private val redownloadPresenter = presenters.redownloadGame.present(this)
    private val rediscoverPresenter = presenters.rediscoverGame.present(this)
    private val editGamePresenter = presenters.editGame.present(this)
    private val renameMoveGamePresenter = presenters.renameMoveGame.present(this)

    private lateinit var game: Game

    override val root = vbox {
        addClass(CommonStyle.popoverMenu)
        val size = 20.0
        item("View", Theme.Icon.view(size)) { setOnAction { controller.viewDetails(game) } }
        separator()
        item("Edit", Theme.Icon.edit(size)) { onAction { editGamePresenter.editGame(game, initialTab = GameDataType.name_) } }
        item("Change Thumbnail", Theme.Icon.thumbnail(size)) { 
            onAction { editGamePresenter.editGame(game, initialTab = GameDataType.thumbnail) }
        }
        separator()
        item("Tag", Theme.Icon.tag(size)) { onAction { tagGamePresenter.tagGame(game) } }
        separator()
        item("Re-Download", Theme.Icon.download(size)) {
            enableWhen { enabledProperty }
            onAction { redownloadPresenter.redownloadGame(game) }
        }
        item("Re-Discover", Theme.Icon.search(size)) {
            enableWhen { enabledProperty }
            dropDownMenu(PopOver.ArrowLocation.LEFT_TOP, closeOnClick = false) {
                discoverGameChooseResultsMenu()
            }
            onAction { rediscoverPresenter.rediscoverGame(game) }
        }
        separator()
        item("Rename/Move Folder", Theme.Icon.folder(size)) {
            onAction { renameMoveGamePresenter.renameMove(game) }
        }
        separator()
        item("Delete", Theme.Icon.delete(size)) { onAction { deleteGamePresenter.deleteGame(game) } }
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

    override fun showTagGameView(game: Game) = tagGameView.show(game)
    override fun showConfirmDeleteGame(game: Game) = DeleteGameView.showConfirmDeleteGame(game)
    override fun showEditGameView(game: Game, initialTab: GameDataType) = editGameView.show(game, initialTab)
    override fun showRenameMoveGameView(game: Game, initialName: String) = renameMoveGameView.show(game, initialName)
}