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

package com.gitlab.ykrasik.gamedex.javafx.game.common

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.GameDataType
import com.gitlab.ykrasik.gamedex.app.api.game.common.DeleteGameChoice
import com.gitlab.ykrasik.gamedex.app.api.game.common.ViewCanDeleteGame
import com.gitlab.ykrasik.gamedex.app.api.game.common.ViewCanEditGame
import com.gitlab.ykrasik.gamedex.app.api.game.tag.ViewCanTagGame
import com.gitlab.ykrasik.gamedex.javafx.dialog.areYouSureDialog
import com.gitlab.ykrasik.gamedex.javafx.game.edit.EditGameDataFragment
import com.gitlab.ykrasik.gamedex.javafx.game.tag.JavaFxTagGameView
import com.gitlab.ykrasik.gamedex.javafx.jfxCheckBox
import javafx.beans.property.SimpleBooleanProperty

/**
 * User: ykrasik
 * Date: 02/05/2018
 * Time: 22:08
 */
// FIXME: Make TagFragment a view and have it implement this interface
object TagGameViewShower : ViewCanTagGame {
    override fun showTagGameView(game: Game) = JavaFxTagGameView().show(game)
}

// FIXME: Make EditFragment a view and have it implement this interface
object EditGameViewShower : ViewCanEditGame {
    override fun showEditGameView(game: Game, initialTab: GameDataType) =
        EditGameDataFragment(game, initialTab).show()
}

object DeleteGameView : ViewCanDeleteGame {
    override fun showConfirmDeleteGame(game: Game): DeleteGameChoice {
        val fromFileSystem = SimpleBooleanProperty(false)
        val confirm = areYouSureDialog("Delete game '${game.name}'?") {
            jfxCheckBox(fromFileSystem, "From File System")
        }
        return if (confirm) {
            DeleteGameChoice.Confirm(fromFileSystem.value)
        } else {
            DeleteGameChoice.Cancel
        }
    }
}