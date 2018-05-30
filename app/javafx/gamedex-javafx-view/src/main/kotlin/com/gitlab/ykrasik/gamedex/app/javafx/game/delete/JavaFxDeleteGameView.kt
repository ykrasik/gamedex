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

package com.gitlab.ykrasik.gamedex.app.javafx.game.delete

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.app.api.game.DeleteGameView
import com.gitlab.ykrasik.gamedex.app.api.util.BroadcastEventChannel
import com.gitlab.ykrasik.gamedex.javafx.dialog.areYouSureDialogContainer
import com.gitlab.ykrasik.gamedex.javafx.jfxCheckBox
import com.gitlab.ykrasik.gamedex.javafx.screen.PresentableView
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import tornadofx.getValue
import tornadofx.setValue
import tornadofx.stringBinding

/**
 * User: ykrasik
 * Date: 02/05/2018
 * Time: 22:08
 */
class JavaFxDeleteGameView : PresentableView(), DeleteGameView {
    private val gameProperty = SimpleObjectProperty<Game>()
    override var game: Game by gameProperty

    override val fromFileSystemChanges = BroadcastEventChannel<Boolean>()
    private val fromFileSystemProperty = SimpleBooleanProperty(false).eventOnChange(fromFileSystemChanges)
    override var fromFileSystem by fromFileSystemProperty

    override val acceptActions = BroadcastEventChannel<Unit>()
    override val cancelActions = BroadcastEventChannel<Unit>()

    init {
        titleProperty.bind(gameProperty.stringBinding { "Delete Game '${it?.name}'?"})
        viewRegistry.register(this)
    }

    override val root = areYouSureDialogContainer(acceptActions, cancelActions, titleProperty) {
        jfxCheckBox(fromFileSystemProperty, "From File System")
    }
}