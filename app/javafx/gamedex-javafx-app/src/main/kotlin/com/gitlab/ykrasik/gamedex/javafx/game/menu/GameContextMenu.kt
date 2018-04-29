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
import com.gitlab.ykrasik.gamedex.app.javafx.game.discover.discoverGameChooseResultsMenu
import com.gitlab.ykrasik.gamedex.javafx.*
import com.gitlab.ykrasik.gamedex.javafx.game.GameController
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
        item("Edit", Theme.Icon.edit(size)) {
            setOnAction {
                javaFx {
                    controller.editDetails(game)
                }
            }
        }
        item("Change Thumbnail", Theme.Icon.thumbnail(size)) {
            setOnAction {
                javaFx {
                    controller.editDetails(game, initialTab = GameDataType.thumbnail)
                }
            }
        }
        separator()
        item("Tag", Theme.Icon.tag(size)) {
            setOnAction {
                javaFx {
                    controller.tag(game)
                }
            }
        }
        separator()
        item("Refresh", Theme.Icon.refresh(size)) {
            enableWhen { controller.canRunLongTask }
            setOnAction {
                javaFx {
                    controller.refreshGame(game)
                }
            }
        }
        item("Re-Discover", Theme.Icon.search(size)) {
            enableWhen { controller.canRunLongTask }
            dropDownMenu(PopOver.ArrowLocation.LEFT_TOP, closeOnClick = false) {
                discoverGameChooseResultsMenu()
            }
            setOnAction {
                javaFx {
                    controller.searchGame(game)
                }
            }
        }
        separator()
        item("Rename/Move Folder", Theme.Icon.folder(size)) {
            setOnAction {
                javaFx {
                    controller.renameFolder(game)
                }
            }
        }
        separator()
        item("Delete", Theme.Icon.delete(size)) {
            setOnAction {
                javaFx {
                    controller.delete(game)
                }
            }
        }
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