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
import com.gitlab.ykrasik.gamedex.LibraryData
import com.gitlab.ykrasik.gamedex.Platform
import com.gitlab.ykrasik.gamedex.app.api.library.EditLibraryView
import com.gitlab.ykrasik.gamedex.app.api.presenters
import com.gitlab.ykrasik.gamedex.javafx.*
import com.gitlab.ykrasik.gamedex.javafx.screen.PresentableView
import javafx.beans.property.SimpleBooleanProperty
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
    private val canChangePlatformProperty = SimpleBooleanProperty(false)
    override var canChangePlatform by canChangePlatformProperty

    private var initialLibraryProperty = SimpleObjectProperty<Library?>(null)
    override var initialLibrary by initialLibraryProperty

    private val viewModel = LibraryViewModel()
    override var name by viewModel.nameProperty
    override var path by viewModel.pathProperty
    override var platform by viewModel.platformProperty

    private val nameValidationErrorProperty = SimpleStringProperty(null)
    override var nameValidationError by nameValidationErrorProperty

    private val pathValidationErrorProperty = SimpleStringProperty(null)
    override var pathValidationError by pathValidationErrorProperty

    private var dataToReturn: LibraryData? = null

    private val presenter = presenters.editLibraryView.present(this)

    init {
        titleProperty.bind(initialLibraryProperty.stringBinding { if (it == null) "Add New Library" else "Edit Library '${it.name}'" })
        nameValidationErrorProperty.onChange { viewModel.validate() }
        pathValidationErrorProperty.onChange { viewModel.validate() }
    }

    override val root = borderpane {
        top {
            toolbar {
                acceptButton {
                    enableWhen { nameValidationErrorProperty.isNull.and(pathValidationErrorProperty.isNull) }
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
            validator(ValidationTrigger.None) {
                pathValidationError?.let { error(it) }
            }
        }
        jfxButton("Browse", Theme.Icon.folderOpen(17.0)) {
            presentOnAction { presenter.onBrowse() }
        }
    }

    private fun Fieldset.nameField() = field("Name") {
        textfield(viewModel.nameProperty) {
            validator(ValidationTrigger.None) {
                nameValidationError?.let { error(it) }
            }
        }
    }

    private fun Fieldset.platformField() = field("Platform") {
        enableWhen { canChangePlatformProperty }
        platformComboBox(viewModel.platformProperty)
    }

    fun show(library: Library?): LibraryData? {
        presenter.onShown(library)
        openModal(block = true)
        return dataToReturn
    }

    override fun close(data: LibraryData?) {
        dataToReturn = data
        close()
    }

    override fun selectDirectory(initialDirectory: File?) = chooseDirectory("Select Library Folder...", initialDirectory)

    private inner class LibraryViewModel : ViewModel() {
        val nameProperty = presentableProperty({ presenter.onNameChanged(it) }, { SimpleStringProperty("") })
        val pathProperty = presentableProperty({ presenter.onPathChanged(it) }, { SimpleStringProperty("") })
        val platformProperty = presentableProperty({ presenter.onPlatformChanged(it) }, { SimpleObjectProperty(Platform.pc) })
    }
}