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

package com.gitlab.ykrasik.gamedex.app.javafx.game.rename

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.Library
import com.gitlab.ykrasik.gamedex.app.api.game.RenameMoveGameView
import com.gitlab.ykrasik.gamedex.app.api.util.channel
import com.gitlab.ykrasik.gamedex.javafx.Icons
import com.gitlab.ykrasik.gamedex.javafx.control.*
import com.gitlab.ykrasik.gamedex.javafx.state
import com.gitlab.ykrasik.gamedex.javafx.theme.header
import com.gitlab.ykrasik.gamedex.javafx.userMutableState
import com.gitlab.ykrasik.gamedex.javafx.view.ConfirmationWindow
import com.gitlab.ykrasik.gamedex.util.IsValid
import com.gitlab.ykrasik.gamedex.util.browse
import javafx.beans.property.SimpleObjectProperty
import javafx.geometry.HPos
import javafx.scene.layout.Priority
import tornadofx.*
import java.io.File

/**
 * User: ykrasik
 * Date: 11/06/2017
 * Time: 19:47
 */
class JavaFxRenameMoveGameView : ConfirmationWindow(icon = Icons.folderEdit), RenameMoveGameView {
    override var initialName: String? = null

    private val gameProperty = SimpleObjectProperty(Game.Null)
    override var game by gameProperty

    override val possibleLibraries = mutableListOf<Library>().observable()

    override val library = userMutableState(Library.Null)
    override val path = userMutableState("")
    override val name = userMutableState("")
    override val nameIsValid = state(IsValid.valid)

    override val selectDirectoryActions = channel<Unit>()
    override val browseToGameActions = channel<Unit>()

    init {
        titleProperty.bind(gameProperty.stringBinding { "Rename/Move '${it!!.name}'" })
        register()
    }

    override val root = borderpane {
        minWidth = 700.0
        minHeight = 100.0
        top = confirmationToolbar()
        center {
            form {
                paddingAll = 10
                fieldset("From") {
                    horizontalField {
                        jfxButton {
                            textProperty().bind(gameProperty.stringBinding { it!!.path.toString() })
                            action(browseToGameActions)
                        }
                    }
                }
                fieldset("To") {
                    horizontalField {
                        gridpane {
                            hgap = 5.0
                            header("Library") { gridpaneConstraints { columnRowIndex(0, 0); hAlignment = HPos.CENTER } }
                            popoverComboMenu(
                                possibleItems = possibleLibraries,
                                selectedItemProperty = library.property,
                                text = { it.path.path }
                            ).apply {
                                gridpaneConstraints { columnRowIndex(0, 1); hAlignment = HPos.LEFT }
                            }

                            header("Path") { gridpaneConstraints { columnRowIndex(1, 0); hAlignment = HPos.CENTER } }
                            jfxButton {
                                gridpaneConstraints { columnRowIndex(1, 1); hAlignment = HPos.LEFT }
                                useMaxWidth = true
                                textProperty().bind(path.property.stringBinding { if (it.isNullOrEmpty()) File.separator else it })
                                action(selectDirectoryActions)
                            }

                            header("Name") { gridpaneConstraints { columnRowIndex(2, 0); hAlignment = HPos.CENTER } }
                            jfxTextField(name.property, promptText = "Enter Name...") {
                                gridpaneConstraints { columnRowIndex(2, 1); hAlignment = HPos.LEFT; hGrow = Priority.ALWAYS }
                                validWhen(nameIsValid)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun selectDirectory(initialDirectory: File): File? = chooseDirectory("Browse Path...", initialDirectory)

    override fun browseTo(dir: File) = browse(dir)
}