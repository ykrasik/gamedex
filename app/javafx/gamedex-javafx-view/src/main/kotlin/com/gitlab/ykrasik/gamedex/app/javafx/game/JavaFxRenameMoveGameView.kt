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
import com.gitlab.ykrasik.gamedex.Library
import com.gitlab.ykrasik.gamedex.app.api.file.ViewCanOpenFile
import com.gitlab.ykrasik.gamedex.app.api.game.RenameMoveGameView
import com.gitlab.ykrasik.gamedex.app.api.util.broadcastFlow
import com.gitlab.ykrasik.gamedex.javafx.binding
import com.gitlab.ykrasik.gamedex.javafx.control.jfxButton
import com.gitlab.ykrasik.gamedex.javafx.control.jfxTextField
import com.gitlab.ykrasik.gamedex.javafx.control.validWhen
import com.gitlab.ykrasik.gamedex.javafx.mutableStateFlow
import com.gitlab.ykrasik.gamedex.javafx.theme.*
import com.gitlab.ykrasik.gamedex.javafx.typesafeStringBinding
import com.gitlab.ykrasik.gamedex.javafx.view.ConfirmationWindow
import com.gitlab.ykrasik.gamedex.javafx.viewMutableStateFlow
import com.gitlab.ykrasik.gamedex.util.IsValid
import javafx.scene.layout.Priority
import tornadofx.*
import java.io.File

/**
 * User: ykrasik
 * Date: 11/06/2017
 * Time: 19:47
 */
class JavaFxRenameMoveGameView : ConfirmationWindow(icon = Icons.folderEdit), RenameMoveGameView, ViewCanOpenFile {
    override val initialName = viewMutableStateFlow<String?>(null, debugName = "initialName")

    override val game = viewMutableStateFlow(Game.Null, debugName = "game")

    override val targetPath = viewMutableStateFlow("", debugName = "targetPath")
    override val targetPathIsValid = mutableStateFlow(IsValid.valid, debugName = "targetPathIsValid")
    override val targetPathLibrary = mutableStateFlow(Library.Null, debugName = "targetPathLibrary")

    override val sanitizeTargetPathActions = broadcastFlow<Unit>()

    override val browseActions = broadcastFlow<Unit>()
    override val openFileActions = broadcastFlow<File>()

    init {
        titleProperty.bind(game.property.typesafeStringBinding { "Rename/Move '${it.name}'" })
        register()
    }

    override val root = borderpane {
        minWidth = 700.0
        minHeight = 100.0
        top = confirmationToolbar()
        center {
            gridpane {
                hgap = 10.0
                vgap = 14.0
                paddingAll = 40
                row {
                    subHeader("From")
                    label {
                        textProperty().bind(game.property.typesafeStringBinding { it.library.name })
                        graphicProperty().bind(game.property.binding { it.library.platformOrNull?.logo })
                    }
                    jfxButton {
                        textProperty().bind(game.property.typesafeStringBinding { it.path.path })
                        action(openFileActions) { game.v.path }
                    }
                }
                row {
                    subHeader("To")
                    label {
                        textProperty().bind(targetPathLibrary.property.typesafeStringBinding { it.name })
                        graphicProperty().bind(targetPathLibrary.property.binding { it.platformOrNull?.logo })
                    }
                    jfxTextField(targetPath.property, promptText = "Enter Path...") {
                        validWhen(targetPathIsValid)
                        paddingLeft = 6.0
                        gridpaneColumnConstraints { hgrow = Priority.ALWAYS }
                        targetPath.onChangeFromPresenter {
                            requestFocus()
                            end() // Move caret to end of text.
                        }
                    }
                    jfxButton(graphic = Icons.cleanup.size(24)) {
                        tooltip("Sanitize")
                        action(sanitizeTargetPathActions)
                        shortcut("ctrl+s")
                    }
                    browseButton { action(browseActions) }
                }
            }
        }
    }

    override fun browse(initialDirectory: File): File? = chooseDirectory("Browse Path...", initialDirectory)
}