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

package com.gitlab.ykrasik.gamedex.app.javafx.library

import com.gitlab.ykrasik.gamedex.Library
import com.gitlab.ykrasik.gamedex.Platform
import com.gitlab.ykrasik.gamedex.app.api.library.EditLibraryView
import com.gitlab.ykrasik.gamedex.app.api.util.channel
import com.gitlab.ykrasik.gamedex.javafx.*
import com.gitlab.ykrasik.gamedex.javafx.view.PresentableView
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import tornadofx.*
import java.io.File

/**
 * User: ykrasik
 * Date: 12/10/2016
 * Time: 10:56
 */
class JavaFxEditLibraryView : PresentableView(), EditLibraryView {
    private var libraryProperty = SimpleObjectProperty<Library?>(null)
    override var library by libraryProperty

    override val nameChanges = channel<String>()
    override val pathChanges = channel<String>()
    override val platformChanges = channel<Platform>()

    private val viewModel = LibraryViewModel()
    override var name by viewModel.nameProperty
    override var path by viewModel.pathProperty
    override var platform by viewModel.platformProperty

    private inner class LibraryViewModel : ViewModel() {
        val nameProperty = presentableStringProperty(nameChanges)
        val pathProperty = presentableStringProperty(pathChanges)
        val platformProperty = presentableProperty(platformChanges) { SimpleObjectProperty(Platform.pc) }
    }

    private val nameValidationErrorProperty = SimpleStringProperty(null)
    override var nameValidationError by nameValidationErrorProperty

    private val pathValidationErrorProperty = SimpleStringProperty(null)
    override var pathValidationError by pathValidationErrorProperty

    override val browseActions = channel<Unit>()
    override val acceptActions = channel<Unit>()
    override val cancelActions = channel<Unit>()

    init {
        titleProperty.bind(libraryProperty.stringBinding { if (it == null) "Add New Library" else "Edit Library '${it.name}'" })
        viewRegistry.register(this)
    }

    override val root = borderpane {
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
                minWidth = 600.0
                fieldset {
                    pathField()
                    nameField()
                    platformField()
                }.apply { textProperty.bind(titleProperty) }
            }
        }
    }

    private fun Fieldset.pathField() = field("Path") {
        textfield(viewModel.pathProperty) {
            validatorFrom(viewModel, pathValidationErrorProperty)
        }
        jfxButton("Browse", Theme.Icon.folderOpen(17.0)) {
            eventOnAction(browseActions)
        }
    }

    private fun Fieldset.nameField() = field("Name") {
        textfield(viewModel.nameProperty) {
            validatorFrom(viewModel, nameValidationErrorProperty)
        }
    }

    private fun Fieldset.platformField() = field("Platform") {
        enableWhen { libraryProperty.isNull }
        platformComboBox(viewModel.platformProperty)
    }

    override fun selectDirectory(initialDirectory: File?) = chooseDirectory("Select Library Folder...", initialDirectory)
}