/****************************************************************************
 * Copyright (C) 2016-2020 Yevgeny Krasik                                   *
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
import com.gitlab.ykrasik.gamedex.app.api.provider.ViewCanSyncGame
import com.gitlab.ykrasik.gamedex.app.api.provider.ViewCanUpdateGame
import com.gitlab.ykrasik.gamedex.app.api.util.channel
import com.gitlab.ykrasik.gamedex.javafx.control.enableWhen
import com.gitlab.ykrasik.gamedex.javafx.control.jfxButton
import com.gitlab.ykrasik.gamedex.javafx.control.verticalGap
import com.gitlab.ykrasik.gamedex.javafx.state
import com.gitlab.ykrasik.gamedex.javafx.theme.GameDexStyle
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
class GameContextMenu(canView: Boolean = true) : InstallableContextMenu<ViewGameParams>(),
    ViewCanShowGameDetails,
    ViewCanEditGame,
    ViewCanDeleteGame,
    ViewCanRenameMoveGame,
    ViewCanTagGame,
    ViewCanUpdateGame,
    ViewCanSyncGame {

    override val gameChannel = dataChannel.map { it.game }

    override val viewGameDetailsActions = channel<ViewGameParams>()
    override val editGameActions = channel<EditGameParams>()
    override val deleteGameActions = channel<Game>()
    override val renameMoveGameActions = channel<RenameMoveGameParams>()
    override val tagGameActions = channel<Game>()

    override val canUpdateGame = state(IsValid.valid)
    override val updateGameActions = channel<Game>()

    override val canSyncGame = state(IsValid.valid)
    override val syncGameActions = channel<Game>()

    override val root = vbox {
        addClass(GameDexStyle.popOverMenu)
        if (canView) {
            item("View", Icons.view) { action(viewGameDetailsActions) { data } }
            verticalGap()
        }
        item("Edit", Icons.edit) { action { editGame(GameDataType.Name) } }
        item("Change Thumbnail", Icons.thumbnail) { action { editGame(GameDataType.Thumbnail) } }
        verticalGap()
        item("Tag", Icons.tag) { action(tagGameActions) { data.game } }
        verticalGap()
        item("Update", Icons.download) {
            addClass(GameDexStyle.infoButton)
            enableWhen(canUpdateGame)
            action(updateGameActions) { data.game }
        }
        item("Sync", Icons.sync) {
            addClass(GameDexStyle.infoButton)
            enableWhen(canSyncGame)
            action(syncGameActions) { data.game }
        }
        verticalGap()
        item("Rename/Move Folder", Icons.folderEdit) {
            action(renameMoveGameActions) { RenameMoveGameParams(data.game, initialSuggestion = null) }
        }
        item("Delete", Icons.delete) {
            addClass(GameDexStyle.dangerButton)
            action(deleteGameActions) { data.game }
        }
    }

    private inline fun VBox.item(text: String, icon: Node, op: JFXButton.() -> Unit) = jfxButton(text, icon, alignment = Pos.CENTER_LEFT) {
        useMaxWidth = true
        op()
    }

    private fun editGame(initialView: GameDataType) = editGameActions.event(EditGameParams(data.game, initialView))

    init {
        // This view must call init manually because it is not created via 'inject'
        init()

        register()
    }
}