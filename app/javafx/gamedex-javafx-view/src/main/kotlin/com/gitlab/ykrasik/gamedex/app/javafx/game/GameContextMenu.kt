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
import com.gitlab.ykrasik.gamedex.javafx.CommonStyle
import com.gitlab.ykrasik.gamedex.javafx.Theme
import com.gitlab.ykrasik.gamedex.javafx.dropDownMenu
import com.gitlab.ykrasik.gamedex.javafx.jfxButton
import com.gitlab.ykrasik.gamedex.javafx.view.InstallableContextMenu
import com.jfoenix.controls.JFXButton
import javafx.scene.Node
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
        viewRegistry.register(this)
    }

    override val root = vbox {
        addClass(CommonStyle.popoverMenu)
        val size = 20.0
        item("View", Theme.Icon.view(size)) { eventOnAction(showGameDetailsActions) { data } }
        separator()
        item("Edit", Theme.Icon.edit(size)) { setOnAction { editGame(GameDataType.name_) } }
        item("Change Thumbnail", Theme.Icon.thumbnail(size)) { setOnAction { editGame(GameDataType.thumbnail) } }
        separator()
        item("Tag", Theme.Icon.tag(size)) { eventOnAction(tagGameActions) { data } }
        separator()
        item("Re-Download", Theme.Icon.download(size)) {
            enableWhen { enabledProperty }
            eventOnAction(redownloadGameActions) { data }
        }
        item("Re-Discover", Theme.Icon.search(size)) {
            enableWhen { enabledProperty }
            dropDownMenu(PopOver.ArrowLocation.LEFT_TOP, closeOnClick = false) {
                discoverGameChooseResultsMenu()
            }
            eventOnAction(rediscoverGameActions) { data }
        }
        separator()
        item("Rename/Move Folder", Theme.Icon.folder(size)) {
            eventOnAction(renameMoveGameActions) { data to null }
        }
        separator()
        item("Delete", Theme.Icon.delete(size)) { eventOnAction(deleteGameActions) { data } }
    }

    private fun VBox.item(text: String, icon: Node, op: JFXButton.() -> Unit) = jfxButton(text, icon, op = op).apply {
        addClass(CommonStyle.fillAvailableWidth)
    }

    private fun editGame(initialScreen: GameDataType) = editGameActions.event(data to initialScreen)
}