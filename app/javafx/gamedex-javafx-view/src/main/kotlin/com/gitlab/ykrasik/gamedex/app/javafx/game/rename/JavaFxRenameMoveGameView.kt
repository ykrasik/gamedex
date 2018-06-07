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

package com.gitlab.ykrasik.gamedex.app.javafx.game.rename

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.Library
import com.gitlab.ykrasik.gamedex.app.api.game.RenameMoveGameView
import com.gitlab.ykrasik.gamedex.app.api.util.channel
import com.gitlab.ykrasik.gamedex.javafx.*
import com.gitlab.ykrasik.gamedex.javafx.screen.PresentableView
import com.gitlab.ykrasik.gamedex.util.browse
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.geometry.HPos
import javafx.scene.layout.Priority
import tornadofx.*
import java.io.File

/**
 * User: ykrasik
 * Date: 11/06/2017
 * Time: 19:47
 */
class JavaFxRenameMoveGameView : PresentableView(), RenameMoveGameView {
    override val possibleLibraries = mutableListOf<Library>().observable()

    override var initialName: String? = null

    private val gameProperty = SimpleObjectProperty<Game>()
    override var game by gameProperty

    override val libraryChanges = channel<Library>()
    override val pathChanges = channel<String>()
    override val nameChanges = channel<String>()
    private val viewModel = RenameFolderViewModel()

    override var library by viewModel.libraryProperty
    override var path by viewModel.pathProperty
    override var name by viewModel.nameProperty

    private inner class RenameFolderViewModel : ViewModel() {
        val libraryProperty = presentableProperty(libraryChanges) { SimpleObjectProperty<Library>() }
        val pathProperty = presentableStringProperty(pathChanges)
        val nameProperty = presentableStringProperty(nameChanges)
    }

    private val nameValidationErrorProperty = SimpleStringProperty(null)
    override var nameValidationError by nameValidationErrorProperty

    override val selectDirectoryActions = channel<Unit>()
    override val browseToGameActions = channel<Unit>()
    override val acceptActions = channel<Unit>()
    override val cancelActions = channel<Unit>()

    init {
        titleProperty.bind(gameProperty.stringBinding { "Rename/Move ${it?.path}" })
        viewRegistry.register(this)
    }

    override val root = borderpane {
        minWidth = 700.0
        minHeight = 100.0
        top {
            toolbar {
                acceptButton {
                    isDefaultButton = true
                    enableWhen { viewModel.valid }
                    eventOnAction(acceptActions)
                }
                spacer()
                cancelButton {
                    isCancelButton = true
                    eventOnAction(cancelActions)
                }
            }
        }
        center {
            form {
                paddingAll = 20.0
                fieldset {
                    field("From") {
                        jfxButton {
                            textProperty().bind(gameProperty.stringBinding { it?.path?.toString() })
                            eventOnAction(browseToGameActions)
                        }
                    }
                    separator()
                    field("To") {
                        gridpane {
                            header("Library") { gridpaneConstraints { columnRowIndex(0, 0); hAlignment = HPos.CENTER } }
                            popoverComboMenu(
                                possibleItems = possibleLibraries,
                                selectedItemProperty = viewModel.libraryProperty,
                                text = { it.path.path }
                            ).apply {
                                gridpaneConstraints { columnRowIndex(0, 1); hAlignment = HPos.LEFT }
                            }

                            verticalSeparator { gridpaneConstraints { columnRowIndex(1, 0); rowSpan = 2 } }

                            header("Path") { gridpaneConstraints { columnRowIndex(2, 0); hAlignment = HPos.CENTER } }
                            jfxButton {
                                gridpaneConstraints { columnRowIndex(2, 1); hAlignment = HPos.LEFT }
                                useMaxWidth = true
                                textProperty().bind(viewModel.pathProperty.map { if (it!!.isEmpty()) File.separator else it })
                                eventOnAction(selectDirectoryActions)
                            }

                            verticalSeparator { gridpaneConstraints { columnRowIndex(3, 0); rowSpan = 2 } }

                            header("Name") { gridpaneConstraints { columnRowIndex(4, 0); hAlignment = HPos.CENTER } }
                            textfield(viewModel.nameProperty) {
                                gridpaneConstraints { columnRowIndex(4, 1); hAlignment = HPos.LEFT; hGrow = Priority.ALWAYS }
                                validatorFrom(viewModel, nameValidationErrorProperty)
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