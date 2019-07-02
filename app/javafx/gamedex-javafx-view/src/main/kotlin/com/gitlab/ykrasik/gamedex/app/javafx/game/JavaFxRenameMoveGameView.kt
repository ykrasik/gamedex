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
import com.gitlab.ykrasik.gamedex.Library
import com.gitlab.ykrasik.gamedex.app.api.file.ViewCanOpenFile
import com.gitlab.ykrasik.gamedex.app.api.game.RenameMoveGameView
import com.gitlab.ykrasik.gamedex.app.api.util.channel
import com.gitlab.ykrasik.gamedex.javafx.binding
import com.gitlab.ykrasik.gamedex.javafx.control.jfxButton
import com.gitlab.ykrasik.gamedex.javafx.control.jfxTextField
import com.gitlab.ykrasik.gamedex.javafx.control.validWhen
import com.gitlab.ykrasik.gamedex.javafx.state
import com.gitlab.ykrasik.gamedex.javafx.theme.Icons
import com.gitlab.ykrasik.gamedex.javafx.theme.browseButton
import com.gitlab.ykrasik.gamedex.javafx.theme.logo
import com.gitlab.ykrasik.gamedex.javafx.theme.subHeader
import com.gitlab.ykrasik.gamedex.javafx.userMutableState
import com.gitlab.ykrasik.gamedex.javafx.view.ConfirmationWindow
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
    override val initialName = userMutableState<String?>(null)

    override val game = userMutableState(Game.Null)

    override val newPath = userMutableState("")
    override val newPathIsValid = state(IsValid.valid)
    override val newPathLibrary = state(Library.Null)

    override val browseActions = channel<Unit>()
    override val openFileActions = channel<File>()

    init {
        titleProperty.bind(game.property.stringBinding { "Rename/Move '${it!!.name}'" })
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
                        textProperty().bind(game.property.stringBinding { it!!.library.name })
                        graphicProperty().bind(game.property.binding { it!!.library.platformOrNull?.logo })
                    }
                    jfxButton {
                        textProperty().bind(game.property.stringBinding { it!!.path.path })
                        action(openFileActions) { game.value.path }
                    }
                }
                row {
                    subHeader("To")
                    label {
                        textProperty().bind(newPathLibrary.property.stringBinding { it!!.name })
                        graphicProperty().bind(newPathLibrary.property.binding { it!!.platformOrNull?.logo })
                    }
                    jfxTextField(newPath.property, promptText = "Enter Path...") {
                        validWhen(newPathIsValid)
                        paddingLeft = 6.0
                        gridpaneColumnConstraints { hgrow = Priority.ALWAYS }
                        sceneProperty().onChange {
                            if (it != null) {
                                requestFocus()
                                end() // Move caret to end of text.
                            }
                        }
                    }
                    browseButton { action(browseActions) }
                }
            }
        }
    }

    override fun browse(initialDirectory: File): File? = chooseDirectory("Browse Path...", initialDirectory)
}