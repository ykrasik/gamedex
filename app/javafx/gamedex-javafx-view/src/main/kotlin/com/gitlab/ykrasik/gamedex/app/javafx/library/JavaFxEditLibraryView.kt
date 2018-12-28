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
import com.gitlab.ykrasik.gamedex.javafx.Icons
import com.gitlab.ykrasik.gamedex.javafx.control.*
import com.gitlab.ykrasik.gamedex.javafx.size
import com.gitlab.ykrasik.gamedex.javafx.state
import com.gitlab.ykrasik.gamedex.javafx.userMutableState
import com.gitlab.ykrasik.gamedex.javafx.view.ConfirmationWindow
import com.gitlab.ykrasik.gamedex.util.IsValid
import javafx.beans.property.SimpleObjectProperty
import tornadofx.*
import java.io.File

/**
 * User: ykrasik
 * Date: 12/10/2016
 * Time: 10:56
 */
class JavaFxEditLibraryView : ConfirmationWindow(icon = Icons.edit), EditLibraryView {
    private val libraryProperty = SimpleObjectProperty<Library?>(null)
    override var library by libraryProperty

    override val name = userMutableState("")
    override val nameIsValid = state(IsValid.valid)
    
    override val path = userMutableState("")
    override val pathIsValid = state(IsValid.valid)

    override val platform = userMutableState(Platform.pc)

    override val browseActions = channel<Unit>()

    init {
        titleProperty.bind(libraryProperty.stringBinding { if (it == null) "Add New Library" else "Edit Library '${it.name}'" })
        register()
    }

    override val root = borderpane {
        top = confirmationToolbar()
        center {
            form {
                minWidth = 600.0
                fieldset {
                    pathField()
                    verticalGap()
                    nameField()
                    verticalGap()
                    platformField()
                }.apply { textProperty.bind(titleProperty) }
            }
        }
    }

    private fun Fieldset.pathField() = horizontalField("Path") {
        jfxTextField(path.property, promptText = "Library Path") {
            validWhen(pathIsValid)
        }
        jfxButton("Browse", Icons.folderOpen.size(24)) {
            eventOnAction(browseActions)
        }
    }

    private fun Fieldset.nameField() = horizontalField("Name") {
        jfxTextField(name.property, promptText = "Library Name") {
            validWhen(nameIsValid)
        }
    }

    private fun Fieldset.platformField() = horizontalField("Platform") {
        enableWhen { libraryProperty.isNull }
        platformComboBox(platform.property)
    }

    override fun selectDirectory(initialDirectory: File?) = chooseDirectory("Select Library Folder...", initialDirectory)
}