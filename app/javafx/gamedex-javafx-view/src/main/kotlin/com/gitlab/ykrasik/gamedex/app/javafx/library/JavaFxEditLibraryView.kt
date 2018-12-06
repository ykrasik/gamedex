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
import com.gitlab.ykrasik.gamedex.app.api.util.IsValid
import com.gitlab.ykrasik.gamedex.app.api.util.channel
import com.gitlab.ykrasik.gamedex.javafx.Icons
import com.gitlab.ykrasik.gamedex.javafx.acceptButton
import com.gitlab.ykrasik.gamedex.javafx.cancelButton
import com.gitlab.ykrasik.gamedex.javafx.control.*
import com.gitlab.ykrasik.gamedex.javafx.size
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
    private val libraryProperty = SimpleObjectProperty<Library?>(null)
    override var library by libraryProperty

    private val canAcceptProperty = SimpleObjectProperty(IsValid.valid)
    override var canAccept by canAcceptProperty

    override val nameChanges = channel<String>()
    private val nameProperty = SimpleStringProperty("").eventOnChange(nameChanges)
    override var name by nameProperty

    private val nameIsValidProperty = SimpleObjectProperty(IsValid.valid)
    override var nameIsValid by nameIsValidProperty
    
    override val pathChanges = channel<String>()
    private val pathProperty = SimpleStringProperty("").eventOnChange(pathChanges)
    override var path by pathProperty

    private val pathIsValidProperty = SimpleObjectProperty(IsValid.valid)
    override var pathIsValid by pathIsValidProperty

    override val platformChanges = channel<Platform>()
    private val platformProperty = SimpleObjectProperty<Platform>(Platform.pc).eventOnChange(platformChanges)
    override var platform by platformProperty

    override val browseActions = channel<Unit>()
    override val acceptActions = channel<Unit>()
    override val cancelActions = channel<Unit>()

    init {
        titleProperty.bind(libraryProperty.stringBinding { if (it == null) "Add New Library" else "Edit Library '${it.name}'" })
        viewRegistry.onCreate(this)
    }

    override val root = borderpane {
        top {
            toolbar {
                cancelButton { eventOnAction(cancelActions) }
                spacer()
                acceptButton {
                    enableWhen(canAcceptProperty)
                    eventOnAction(acceptActions)
                }
            }
        }
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

    private fun Fieldset.pathField() = field("Path") {
        jfxTextField(pathProperty, promptText = "Library Path") {
            validWhen(pathIsValidProperty)
        }
        jfxButton("Browse", Icons.folderOpen.size(24)) {
            eventOnAction(browseActions)
        }
    }

    private fun Fieldset.nameField() = field("Name") {
        jfxTextField(nameProperty, promptText = "Library Name") {
            validWhen(nameIsValidProperty)
        }
    }

    private fun Fieldset.platformField() = field("Platform") {
        enableWhen { libraryProperty.isNull }
        platformComboBox(platformProperty)
    }

    override fun selectDirectory(initialDirectory: File?) = chooseDirectory("Select Library Folder...", initialDirectory)
}