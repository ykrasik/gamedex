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
import com.gitlab.ykrasik.gamedex.app.api.file.ViewCanOpenFile
import com.gitlab.ykrasik.gamedex.app.api.game.ViewGameParams
import com.gitlab.ykrasik.gamedex.app.api.util.channel
import com.gitlab.ykrasik.gamedex.javafx.addComponent
import com.gitlab.ykrasik.gamedex.javafx.control.prettyListCell
import com.gitlab.ykrasik.gamedex.javafx.control.prettyListView
import com.gitlab.ykrasik.gamedex.javafx.screenBounds
import com.gitlab.ykrasik.gamedex.javafx.typeSafeOnChange
import com.gitlab.ykrasik.gamedex.javafx.view.PresentableView
import javafx.collections.ObservableList
import javafx.scene.layout.Priority
import tornadofx.hbox
import tornadofx.hgrow
import tornadofx.vbox
import tornadofx.vgrow
import java.io.File

/**
 * User: ykrasik
 * Date: 10/06/2017
 * Time: 16:25
 */
class GameListView(games: ObservableList<Game>) : PresentableView("Game List"), ViewCanOpenFile {
    private val gameContextMenu = GameContextMenu(canView = false)

    private val gameDetailsView = JavaFxGameDetailsView(
        canClose = false,
        imageFitWidth = screenBounds.width / 3,
        imageFitHeight = screenBounds.height * 2 / 3, // TODO: This sucks, find a way to make this be dynamic.
        maxDetailsWidth = screenBounds.width / 4
    )

    override val openFileActions = channel<File>()

    private val gamesView = prettyListView(games) {
        prefWidth = 900.0
        maxWidth = prefWidth
        prettyListCell { game ->
            gameContextMenu.install(this) { ViewGameParams(game, emptyList()) }
            text = null
            graphic = GameDetailsSummaryBuilder(game) {
                nameOp = { maxWidth = 550.0 }
                pathOp = { maxWidth = 550.0 }
            }.build()
        }
    }

    override val root = hbox {
        // Left
        vbox {
            maxWidth = screenBounds.width / 2
            add(gamesView.apply { vgrow = Priority.ALWAYS })
        }

        // Right
        addComponent(gameDetailsView) {
            root.hgrow = Priority.ALWAYS
            gamesView.selectionModel.selectedItemProperty().typeSafeOnChange {
                if (it != null) {
                    gameParams *= ViewGameParams(it, emptyList())
                }
            }
        }
    }

    init {
        // This view must call init manually because it is not created via 'inject'
        init()

        register()
    }
}