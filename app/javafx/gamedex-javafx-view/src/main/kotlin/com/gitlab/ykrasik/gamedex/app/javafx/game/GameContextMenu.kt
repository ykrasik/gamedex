/****************************************************************************
 * Copyright (C) 2016-2019 Yevgeny Krasik                                   *
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
import com.gitlab.ykrasik.gamedex.app.api.provider.ViewCanRedownloadGame
import com.gitlab.ykrasik.gamedex.app.api.provider.ViewCanResyncGame
import com.gitlab.ykrasik.gamedex.app.api.util.channel
import com.gitlab.ykrasik.gamedex.javafx.control.enableWhen
import com.gitlab.ykrasik.gamedex.javafx.control.jfxButton
import com.gitlab.ykrasik.gamedex.javafx.control.verticalGap
import com.gitlab.ykrasik.gamedex.javafx.state
import com.gitlab.ykrasik.gamedex.javafx.theme.CommonStyle
import com.gitlab.ykrasik.gamedex.javafx.theme.Icons
import com.gitlab.ykrasik.gamedex.javafx.view.InstallableContextMenu
import com.gitlab.ykrasik.gamedex.util.IsValid
import com.jfoenix.controls.JFXButton
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.layout.VBox
import tornadofx.action
import tornadofx.addClass
import tornadofx.useMaxWidth
import tornadofx.vbox

/**
 * User: ykrasik
 * Date: 10/06/2017
 * Time: 21:20
 */
// TODO: Allow adding extra buttons, like for report screen.
class GameContextMenu : InstallableContextMenu<Game>(),
    ViewCanShowGameDetails, ViewCanEditGame, ViewCanDeleteGame,
    ViewCanRenameMoveGame, ViewCanTagGame, ViewCanRedownloadGame, ViewCanResyncGame {

    override val showGameDetailsActions = channel<Game>()
    override val editGameActions = channel<Pair<Game, GameDataType>>()
    override val deleteGameActions = channel<Game>()
    override val renameMoveGameActions = channel<Pair<Game, String?>>()
    override val tagGameActions = channel<Game>()
    override val redownloadGameActions = channel<Game>()

    override val canResyncGame = state(IsValid.valid)
    override val resyncGameActions = channel<Game>()

    init {
        register()
    }

    override val root = vbox {
        addClass(CommonStyle.popoverMenu)
        item("View", Icons.view) { action(showGameDetailsActions) { data } }
        verticalGap()
        item("Edit", Icons.edit) { action { editGame(GameDataType.Name) } }
        item("Change Thumbnail", Icons.thumbnail) { action { editGame(GameDataType.Thumbnail) } }
        verticalGap()
        item("Tag", Icons.tag) { action(tagGameActions) { data } }
        verticalGap()
        item("Re-Download", Icons.download) {
            addClass(CommonStyle.infoButton)
            action(redownloadGameActions) { data }
        }
        item("Re-Sync", Icons.sync) {
            addClass(CommonStyle.infoButton)
            enableWhen(canResyncGame)
            action(resyncGameActions) { data }
        }
        verticalGap()
        item("Rename/Move Folder", Icons.folderEdit) { action(renameMoveGameActions) { data to null } }
        item("Delete", Icons.delete) {
            addClass(CommonStyle.dangerButton)
            action(deleteGameActions) { data }
        }
    }

    private inline fun VBox.item(text: String, icon: Node, op: JFXButton.() -> Unit) = jfxButton(text, icon, alignment = Pos.CENTER_LEFT) {
        useMaxWidth = true
        op()
    }

    private fun editGame(initialScreen: GameDataType) = editGameActions.event(data to initialScreen)
}