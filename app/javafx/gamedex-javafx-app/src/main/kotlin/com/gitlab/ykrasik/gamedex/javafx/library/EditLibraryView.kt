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
import com.gitlab.ykrasik.gamedex.core.api.library.AddLibraryRequest
import com.gitlab.ykrasik.gamedex.core.api.library.LibraryRepository
import com.gitlab.ykrasik.gamedex.core.general.GeneralUserConfig
import com.gitlab.ykrasik.gamedex.core.userconfig.UserConfigRepository
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
class EditLibraryView : View() {
    private val libraryRepository: LibraryRepository by di()
    private val userConfigRepository: UserConfigRepository by di()
    private val generalUserConfig = userConfigRepository[GeneralUserConfig::class]

    private val libraryProperty = SimpleObjectProperty<Library?>(null)
    private val library by libraryProperty

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
            validator { path ->
                when {
                    path.isNullOrBlank() -> error("Path is required!")
                    !path!!.toFile().isDirectory -> error("Path doesn't exist!")
                    libraryRepository.libraries.any { it != library && it.path == path.toFile() } -> error("Path already in use!")
                    else -> null
                }
            }
        }
        jfxButton("Browse", Theme.Icon.search(17.0)) { setOnAction { browse() } }
    }

    private fun Fieldset.nameField() = field("Name") {
        textfield(model.nameProperty) {
            validator { name ->
                when {
                    name.isNullOrBlank() -> error("Name is required!")
                    libraryRepository.libraries.any { it != library && it.name == name && it.platform == model.platform } ->
                        error("Name already in use for this platform!")
                    else -> null
                }
            }
        }
    }

    private fun Fieldset.platformField() = field("Platform") {
        disableWhen { libraryProperty.isNotNull }
        platformComboBox(model.platformProperty)
    }

    init {
        titleProperty.bind(libraryProperty.stringBinding { if (it == null) "Add New Library" else "Edit Library '${it.name}'" })
        libraryProperty.onChange {
            model.path = it?.path?.toString() ?: ""
            model.name = it?.name ?: ""
            model.platform = it?.platform ?: Platform.pc
        }
        model.platformProperty.onChange { model.validate() }
        model.validate(decorateErrors = false)
    }

    override fun onDock() {
        if (library == null) browse()
    }

    private fun browse() {
        val directory = chooseDirectory("Browse Library Path...", initialDirectory = generalUserConfig.prevDirectory.existsOrNull())
            ?: return
        generalUserConfig.prevDirectory = directory
        model.path = directory.path
        model.name = directory.name
    }

    private fun close(accept: Boolean) {
        this.accept = accept
        close()
    }

    fun show(library: Library?): Choice {
        libraryProperty.value = library
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

        val platformProperty = bind { SimpleObjectProperty(Platform.pc) }
        var platform by platformProperty

        fun toRequest() = AddLibraryRequest(path = path.toFile(), data = LibraryData(platform = platform, name = name))

        override fun toString() = "LibraryViewModel(name = $name, platform = $platform, path = $path)"
    }
}