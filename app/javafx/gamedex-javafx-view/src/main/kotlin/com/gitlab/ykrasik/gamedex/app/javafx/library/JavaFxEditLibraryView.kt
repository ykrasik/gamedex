/****************************************************************************
 * Copyright (C) 2016-2020 Yevgeny Krasik                                   *
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
import com.gitlab.ykrasik.gamedex.LibraryType
import com.gitlab.ykrasik.gamedex.Platform
import com.gitlab.ykrasik.gamedex.app.api.library.EditLibraryView
import com.gitlab.ykrasik.gamedex.app.api.util.channel
import com.gitlab.ykrasik.gamedex.javafx.control.*
import com.gitlab.ykrasik.gamedex.javafx.state
import com.gitlab.ykrasik.gamedex.javafx.theme.Icons
import com.gitlab.ykrasik.gamedex.javafx.theme.browseButton
import com.gitlab.ykrasik.gamedex.javafx.theme.icon
import com.gitlab.ykrasik.gamedex.javafx.userMutableState
import com.gitlab.ykrasik.gamedex.javafx.view.ConfirmationWindow
import com.gitlab.ykrasik.gamedex.util.IsValid
import tornadofx.*
import java.io.File

/**
 * User: ykrasik
 * Date: 12/10/2016
 * Time: 10:56
 */
class JavaFxEditLibraryView : ConfirmationWindow(icon = Icons.edit), EditLibraryView {
    override val library = userMutableState<Library?>(null)

    override val name = userMutableState("")
    override val nameIsValid = state(IsValid.valid)

    override val path = userMutableState("")
    override val pathIsValid = state(IsValid.valid)

    override val type = userMutableState(LibraryType.Digital)
    override val canChangeType = state(IsValid.valid)

    override val platform = userMutableState<Platform?>(null)
    override val shouldShowPlatform = state(IsValid.valid)
    override val canChangePlatform = state(IsValid.valid)

    override val browseActions = channel<Unit>()

    init {
        titleProperty.bind(library.property.stringBinding { if (it == null) "Add New Library" else "Edit Library" })
        iconProperty.bind(library.property.objectBinding { if (it == null) Icons.add else Icons.edit })
        register()
    }

    override val root = borderpane {
        top = confirmationToolbar()
        center {
            form {
                minWidth = 600.0
                fieldset {
                    nameField()
                    verticalGap()
                    pathField()
                    verticalGap()
                    typeField()
                    verticalGap()
                    platformField()
                }
            }
        }
    }

    private fun Fieldset.pathField() = horizontalField("Path") {
        jfxTextField(path.property, promptText = "Enter Path...") {
            validWhen(pathIsValid)
        }
        browseButton { action(browseActions) }
    }

    private fun Fieldset.nameField() = horizontalField("Name") {
        jfxTextField(name.property, promptText = "Enter Name...") {
            validWhen(nameIsValid)
        }
    }

    private fun Fieldset.typeField() = horizontalField("Type") {
        enableWhen(canChangeType)
        enumComboMenu(
            type.property,
            text = LibraryType::displayName,
            graphic = { it.icon }
        )
    }

    private fun Fieldset.platformField() = horizontalField("Platform") {
        showWhen(shouldShowPlatform)
        enableWhen(canChangePlatform)
        platformComboBox(platform.property)
    }

    override fun browse(initialDirectory: File?) = chooseDirectory("Select Library Folder...", initialDirectory)
}