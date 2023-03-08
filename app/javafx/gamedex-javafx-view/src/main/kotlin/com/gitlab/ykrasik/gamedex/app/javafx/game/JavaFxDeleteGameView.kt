/****************************************************************************
 * Copyright (C) 2016-2023 Yevgeny Krasik                                   *
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
import com.gitlab.ykrasik.gamedex.app.api.game.DeleteGameView
import com.gitlab.ykrasik.gamedex.javafx.control.defaultHbox
import com.gitlab.ykrasik.gamedex.javafx.control.gap
import com.gitlab.ykrasik.gamedex.javafx.control.jfxCheckBox
import com.gitlab.ykrasik.gamedex.javafx.localShortcut
import com.gitlab.ykrasik.gamedex.javafx.theme.Icons
import com.gitlab.ykrasik.gamedex.javafx.typesafeStringBinding
import com.gitlab.ykrasik.gamedex.javafx.view.ConfirmationWindow
import com.gitlab.ykrasik.gamedex.javafx.viewMutableStateFlow
import tornadofx.label

/**
 * User: ykrasik
 * Date: 02/05/2018
 * Time: 22:08
 */
class JavaFxDeleteGameView : ConfirmationWindow(icon = Icons.delete), DeleteGameView {
    override val game = viewMutableStateFlow(Game.Null, debugName = "game")

    override val fromFileSystem = viewMutableStateFlow(false, debugName = "fromFileSystem")

    init {
        titleProperty.bind(game.property.typesafeStringBinding { "Delete '${it.name}'?" })
        register()
    }

    override val root = buildAreYouSure {
        defaultHbox {
            jfxCheckBox(fromFileSystem.property, "From File System") {
                localShortcut(this, "delete")
            }
            gap(10)
            label(game.property.typesafeStringBinding { it.path.toString() })
        }
    }
}
