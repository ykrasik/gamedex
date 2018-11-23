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

package com.gitlab.ykrasik.gamedex.app.javafx.game

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.GameDataType
import com.gitlab.ykrasik.gamedex.app.api.game.*
import com.gitlab.ykrasik.gamedex.app.api.util.channel
import com.gitlab.ykrasik.gamedex.app.javafx.game.discover.discoverGameChooseResultsMenu
import com.gitlab.ykrasik.gamedex.javafx.*
import com.gitlab.ykrasik.gamedex.javafx.view.InstallableContextMenu
import com.jfoenix.controls.JFXButton
import javafx.scene.Node
import javafx.scene.layout.VBox
import org.controlsfx.control.PopOver
import tornadofx.addClass
import tornadofx.vbox

/**
 * User: ykrasik
 * Date: 10/06/2017
 * Time: 21:20
 */
// TODO: Allow adding extra buttons, like for report screen.
class GameContextMenu : InstallableContextMenu<Game>(), ViewCanShowGameDetails, ViewCanEditGame, ViewCanDeleteGame,
    ViewCanRenameMoveGame, ViewCanTagGame, ViewCanRedownloadGame, ViewCanRediscoverGame {
    override val showGameDetailsActions = channel<Game>()
    override val editGameActions = channel<Pair<Game, GameDataType>>()
    override val deleteGameActions = channel<Game>()
    override val renameMoveGameActions = channel<Pair<Game, String?>>()
    override val tagGameActions = channel<Game>()
    override val redownloadGameActions = channel<Game>()
    override val rediscoverGameActions = channel<Game>()

    init {
        viewRegistry.onCreate(this)
    }

    override val root = vbox {
        addClass(CommonStyle.popoverMenu)
        item("View", Icons.view) { eventOnAction(showGameDetailsActions) { data } }
        verticalGap()
        item("Edit", Icons.edit) { setOnAction { editGame(GameDataType.name_) } }
        item("Change Thumbnail", Icons.thumbnail) { setOnAction { editGame(GameDataType.thumbnail) } }
        verticalGap()
        item("Tag", Icons.tag) { eventOnAction(tagGameActions) { data } }
        verticalGap()
        item("Re-Download", Icons.download) {
            eventOnAction(redownloadGameActions) { data }
        }
        item("Re-Sync", Icons.sync) {
            dropDownMenu(PopOver.ArrowLocation.LEFT_TOP, closeOnClick = false) {
                discoverGameChooseResultsMenu()
            }
            eventOnAction(rediscoverGameActions) { data }
        }
        verticalGap()
        item("Rename/Move Folder", Icons.folderEdit) {
            eventOnAction(renameMoveGameActions) { data to null }
        }
        item("Delete", Icons.delete) {
            addClass(CommonStyle.dangerButton)
            eventOnAction(deleteGameActions) { data }
        }
    }

    private fun VBox.item(text: String, icon: Node, op: JFXButton.() -> Unit) = jfxButton(text, icon, op = op).apply {
        addClass(CommonStyle.fillAvailableWidth)
    }

    private fun editGame(initialScreen: GameDataType) = editGameActions.event(data to initialScreen)
}