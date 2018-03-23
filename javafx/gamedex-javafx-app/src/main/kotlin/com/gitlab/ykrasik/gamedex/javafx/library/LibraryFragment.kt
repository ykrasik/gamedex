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

import com.gitlab.ykrasik.gamdex.core.api.library.AddLibraryRequest
import com.gitlab.ykrasik.gamdex.core.api.library.LibraryService
import com.gitlab.ykrasik.gamedex.Library
import com.gitlab.ykrasik.gamedex.LibraryData
import com.gitlab.ykrasik.gamedex.Platform
import com.gitlab.ykrasik.gamedex.core.general.GeneralSettings
import com.gitlab.ykrasik.gamedex.javafx.*
import com.gitlab.ykrasik.gamedex.util.existsOrNull
import com.gitlab.ykrasik.gamedex.util.toFile
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import tornadofx.*

/**
 * User: ykrasik
 * Date: 12/10/2016
 * Time: 10:56
 */
class LibraryFragment(private val library: Library?) : Fragment(if (library == null) "Add New Library" else "Edit Library '${library.name}'") {
    private val libraryService: LibraryService by di()
    private val settings: GeneralSettings by di()

    private val model = LibraryViewModel()
    private var accept = false

    override val root = borderpane {
        top {
            toolbar {
                acceptButton {
                    enableWhen { model.valid }
                    setOnAction { close(accept = true) }
                }
                spacer()
                cancelButton {
                    isCancelButton = true
                    setOnAction { close(accept = false) }
                }
            }
        }
        center {
            form {
                minWidth = 600.0
                fieldset(if (library == null) "Add New Library" else "Edit Library '${library.name}'") {
                    pathField()
                    nameField()
                    platformField()
                }
            }
        }
    }

    private fun Fieldset.pathField() = field("Path") {
        textfield(model.pathProperty) {
            validator { path ->
                when {
                    path.isNullOrBlank() -> error("Path is required!")
                    !path!!.toFile().isDirectory -> error("Path doesn't exist!")
                    libraryService.libraries.any { it != library && it.path == path.toFile() } -> error("Path already in use!")
                    else -> null
                }
            }
            if (library != null) text = library.path.toString()
        }
        jfxButton("Browse", Theme.Icon.search(17.0)) { setOnAction { browse() } }
    }

    private fun Fieldset.nameField() = field("Name") {
        textfield(model.nameProperty) {
            validator { name ->
                when {
                    name.isNullOrBlank() -> error("Name is required!")
                    libraryService.libraries.any { it != library && it.name == name && it.platform == model.platform } ->
                        error("Name already in use for this platform!")
                    else -> null
                }
            }
            if (library != null) text = library.name
        }
    }

    private fun Fieldset.platformField() = field("Platform") {
        isDisable = library != null
        model.platformProperty.value = library?.platform ?: Platform.pc
        model.platformProperty.onChange { model.validate() }
        platformComboBox(model.platformProperty)
    }

    init {
        model.validate(decorateErrors = false)
    }

    override fun onDock() {
        if (library == null) browse()
    }

    private fun browse() {
        val directory = chooseDirectory("Browse Library Path...", initialDirectory = settings.prevDirectory.existsOrNull()) ?: return
        settings.prevDirectory = directory
        model.path = directory.path
        model.name = directory.name
    }

    private fun close(accept: Boolean) {
        this.accept = accept
        close()
    }

    fun show(): Choice {
        openModal(block = true)
        return if (accept && model.commit()) {
            if (library == null) {
                Choice.AddNewLibrary(model.toRequest())
            } else {
                Choice.EditLibrary(library.copy(path = model.path.toFile(), data = library.data.copy(name = model.name)))
            }
        } else {
            Choice.Cancel
        }
    }

    sealed class Choice {
        data class AddNewLibrary(val request: AddLibraryRequest) : Choice()
        data class EditLibrary(val library: Library) : Choice()
        object Cancel : Choice()
    }

    private class LibraryViewModel : ViewModel() {
        val pathProperty = bind { SimpleStringProperty() }
        var path by pathProperty

        val nameProperty = bind { SimpleStringProperty() }
        var name by nameProperty

        val platformProperty = bind { SimpleObjectProperty<Platform>() }
        var platform by platformProperty

        fun toRequest() = AddLibraryRequest(path = path.toFile(), data = LibraryData(platform = platform, name = name))

        override fun toString() = "LibraryViewModel(name = $name, platform = $platform, path = $path)"
    }
}