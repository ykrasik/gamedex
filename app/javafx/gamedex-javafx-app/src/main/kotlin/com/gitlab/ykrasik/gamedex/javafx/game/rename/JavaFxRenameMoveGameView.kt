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

package com.gitlab.ykrasik.gamedex.javafx.game.rename

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.Library
import com.gitlab.ykrasik.gamedex.app.api.game.rename.RenameMoveGameChoice
import com.gitlab.ykrasik.gamedex.app.api.game.rename.RenameMoveGameView
import com.gitlab.ykrasik.gamedex.app.api.presenters
import com.gitlab.ykrasik.gamedex.javafx.*
import com.gitlab.ykrasik.gamedex.javafx.screen.PresentableView
import com.gitlab.ykrasik.gamedex.util.browse
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.geometry.HPos
import javafx.scene.layout.Priority
import javafx.stage.StageStyle
import tornadofx.*
import java.io.File

/**
 * User: ykrasik
 * Date: 11/06/2017
 * Time: 19:47
 */
class JavaFxRenameMoveGameView : PresentableView(), RenameMoveGameView {
    override val possibleLibraries = mutableListOf<Library>().observable()

    private val gameProperty = SimpleObjectProperty<Game>()
    override var game by gameProperty

    private val viewModel = RenameFolderViewModel()
    override var library by viewModel.libraryProperty
    override var path by viewModel.pathProperty
    override var name by viewModel.nameProperty

    private val nameValidationErrorProperty = SimpleStringProperty(null)
    override var nameValidationError by nameValidationErrorProperty

    private val presenter = presenters.renameMoveGameView.present(this)

    private var choice: RenameMoveGameChoice = RenameMoveGameChoice.Cancel

    init {
        titleProperty.bind(gameProperty.stringBinding { "Rename/Move ${it?.path}" })
    }

    override val root = borderpane {
        minWidth = 700.0
        minHeight = 100.0
        top {
            toolbar {
                acceptButton {
                    isDefaultButton = true
                    enableWhen { viewModel.valid }
                    presentOnAction { presenter.onAccept() }
                }
                spacer()
                cancelButton {
                    isCancelButton = true
                    presentOnAction { presenter.onCancel() }
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
                            presentOnAction { presenter.onBrowseToGame() }
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
                                presentOnAction { presenter.onBrowsePath() }
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

    fun show(game: Game, initialName: String): RenameMoveGameChoice {
        presenter.onShown(game, initialName)
        openModal(block = true, stageStyle = StageStyle.UNIFIED)
        return choice
    }

    override fun close(choice: RenameMoveGameChoice) {
        this.choice = choice
        close()
    }

    override fun selectDirectory(initialDirectory: File): File? = chooseDirectory("Browse Path...", initialDirectory)

    override fun browseTo(dir: File) = browse(dir)

    private inner class RenameFolderViewModel : ViewModel() {
        val libraryProperty = presentableProperty({ presenter.onLibraryChanged(it) }) { SimpleObjectProperty<Library>() }
        val pathProperty = presentableProperty({ presenter.onPathChanged(it) }) { SimpleStringProperty("") }
        val nameProperty = presentableProperty({ presenter.onNameChanged(it) }) { SimpleStringProperty("") }
    }
}