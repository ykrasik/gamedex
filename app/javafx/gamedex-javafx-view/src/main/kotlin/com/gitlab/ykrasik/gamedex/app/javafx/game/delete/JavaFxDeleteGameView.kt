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
import com.gitlab.ykrasik.gamedex.app.api.game.delete.DeleteGameView
import com.gitlab.ykrasik.gamedex.app.api.presenters
import com.gitlab.ykrasik.gamedex.javafx.dialog.areYouSureDialog
import com.gitlab.ykrasik.gamedex.javafx.jfxCheckBox
import com.gitlab.ykrasik.gamedex.javafx.screen.presentOnChange
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import tornadofx.getValue
import tornadofx.setValue

/**
 * User: ykrasik
 * Date: 02/05/2018
 * Time: 22:08
 */
object JavaFxDeleteGameView : DeleteGameView {
    private val gameProperty = SimpleObjectProperty<Game>().presentOnChange { presenter.onGameChanged(it) }
    override var game: Game by gameProperty

    private val fromFileSystemProperty = SimpleBooleanProperty(false).presentOnChange { presenter.onFromFileSystemChanged(it) }
    override var fromFileSystem by fromFileSystemProperty

    private val presenter = presenters.deleteGame.present(this)

    fun show(game: Game) {
        JavaFxDeleteGameView.game = game
        val accept = areYouSureDialog("Delete game '${game.name}'?") {
            jfxCheckBox(fromFileSystemProperty, "From File System")
        }
        // Not entirely controlled by the presenter, but good enough.
        if (accept) {
            presenter.onAccept()
        } else {
            presenter.onCancel()
        }
    }

    override fun closeView() {
        // Closed automatically by the dialog
    }
}