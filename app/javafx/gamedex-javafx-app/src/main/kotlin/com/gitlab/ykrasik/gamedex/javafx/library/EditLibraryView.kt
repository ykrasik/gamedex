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

package com.gitlab.ykrasik.gamedex.javafx.library

import com.gitlab.ykrasik.gamedex.Library
import com.gitlab.ykrasik.gamedex.LibraryData
import com.gitlab.ykrasik.gamedex.Platform
import com.gitlab.ykrasik.gamedex.core.api.library.EditLibraryViewModel
import com.gitlab.ykrasik.gamedex.core.api.presenters
import com.gitlab.ykrasik.gamedex.javafx.*
import com.gitlab.ykrasik.gamedex.javafx.screen.PresentableView
import com.gitlab.ykrasik.gamedex.util.toFile
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
class EditLibraryView : PresentableView<EditLibraryViewModel.Event, EditLibraryViewModel.Action, EditLibraryViewModel>(
    "", null, presenters.editLibraryPresenter::present
) {
    private val libraryProperty = SimpleObjectProperty<Library?>(null)
    private val library by libraryProperty

    private val canChangePlatformProperty = SimpleBooleanProperty(false)
    private val nameErrorProperty = SimpleStringProperty(null)
    private val pathErrorProperty = SimpleStringProperty(null)

    private val model = LibraryViewModel()
    private var dataToReturn: LibraryData? = null

    override val root = borderpane {
        top {
            toolbar {
                acceptButton {
                    enableWhen { model.valid }
                    setOnAction { sendEvent(EditLibraryViewModel.Event.AcceptButtonClicked(model.toData())) }
                }
                spacer()
                cancelButton {
                    isCancelButton = true
                    setOnAction { sendEvent(EditLibraryViewModel.Event.CancelButtonClicked) }
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
        textfield(model.pathProperty) {
            textProperty().onChange { sendEvent(EditLibraryViewModel.Event.LibraryPathChanged(model.toData(), library)) }
            validator(ValidationTrigger.None) {
                pathErrorProperty.value?.let { error(it) }
            }
        }
        jfxButton("Browse", Theme.Icon.folderOpen(17.0)) {
            setOnAction { sendEvent(EditLibraryViewModel.Event.BrowseClicked) }
        }
    }

    private fun Fieldset.nameField() = field("Name") {
        textfield(model.nameProperty) {
            textProperty().onChange { sendEvent(EditLibraryViewModel.Event.LibraryNameChanged(model.toData(), library)) }
            validator(ValidationTrigger.None) {
                nameErrorProperty.value?.let { error(it) }
            }
        }
    }

    private fun Fieldset.platformField() = field("Platform") {
        enableWhen { canChangePlatformProperty }
        platformComboBox(model.platformProperty)
        model.platformProperty.onChange { sendEvent(EditLibraryViewModel.Event.LibraryPlatformChanged(model.toData(), library)) }
    }

    init {
        titleProperty.bind(libraryProperty.stringBinding { if (it == null) "Add New Library" else "Edit Library '${it.name}'" })
        libraryProperty.onChange {
            model.path = it?.path?.toString() ?: ""
            model.name = it?.name ?: ""
            model.platform = it?.platform ?: Platform.pc
            model.commit()
        }
        model.validate(decorateErrors = false)
    }

    fun show(library: Library?): LibraryData? {
        whenDocked {
            // This must be done after the view is shown, to allow the viewModel to initialize.
            libraryProperty.value = library
        }
        openModal(block = true)
        return dataToReturn
    }

    override suspend fun EditLibraryViewModel.onPresent() {
        sendEvent(EditLibraryViewModel.Event.Shown(library))
    }

    override suspend fun onAction(action: EditLibraryViewModel.Action) {
        when (action) {
            is EditLibraryViewModel.Action.SetCanChangePlatform -> canChangePlatformProperty.value = action.canChangePlatform
            is EditLibraryViewModel.Action.Browse -> browse(action.initialDirectory)
            is EditLibraryViewModel.Action.SetLibraryData -> {
                model.name = action.data.name
                model.path = action.data.path.toString()
                model.platform = action.data.platform
                model.validate()
            }
            is EditLibraryViewModel.Action.LibraryNameValidationResult -> {
                nameErrorProperty.value = action.error
                model.validate()
            }
            is EditLibraryViewModel.Action.LibraryPathValidationResult -> {
                pathErrorProperty.value = action.error
                model.validate()
            }

            is EditLibraryViewModel.Action.Close -> {
                dataToReturn = action.data
                close()
            }
        }
    }

    private fun browse(initialDirectory: File?) {
        val directory = chooseDirectory("Browse Library Path...", initialDirectory)
        sendEvent(EditLibraryViewModel.Event.BrowseClosed(directory, model.toData(), library))
    }

    private class LibraryViewModel : ViewModel() {
        val pathProperty = bind { SimpleStringProperty("") }
        var path by pathProperty

        val nameProperty = bind { SimpleStringProperty("") }
        var name by nameProperty

        val platformProperty = bind { SimpleObjectProperty(Platform.pc) }
        var platform by platformProperty

        fun toData() = LibraryData(name = name, path = path.toFile(), platform = platform)

        override fun toString() = "LibraryViewModel(name = $name, platform = $platform, path = $path)"
    }
}