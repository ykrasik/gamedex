/****************************************************************************
 * Copyright (C) 2016-2023 Yevgeny Krasik                                   *
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

package com.gitlab.ykrasik.gamedex.app.javafx.maintenance

import com.gitlab.ykrasik.gamedex.app.api.maintenance.ImportDatabaseView
import com.gitlab.ykrasik.gamedex.app.api.util.broadcastFlow
import com.gitlab.ykrasik.gamedex.javafx.control.*
import com.gitlab.ykrasik.gamedex.javafx.mutableStateFlow
import com.gitlab.ykrasik.gamedex.javafx.theme.Icons
import com.gitlab.ykrasik.gamedex.javafx.theme.browseButton
import com.gitlab.ykrasik.gamedex.javafx.view.ConfirmationWindow
import com.gitlab.ykrasik.gamedex.javafx.viewMutableStateFlow
import com.gitlab.ykrasik.gamedex.util.IsValid
import tornadofx.*
import java.io.File

/**
 * User: ykrasik
 * Date: 06/05/2019
 * Time: 08:38
 */
class JavaFxImportDatabaseView : ConfirmationWindow("Import Database", Icons.import), ImportDatabaseView {
    override val importDatabaseFile = viewMutableStateFlow("", debugName = "importDatabaseFile")
    override val importDatabaseFileIsValid = mutableStateFlow(IsValid.valid, debugName = "importDatabaseFileIsValid")

    override val shouldImportLibrary = viewMutableStateFlow(false, debugName = "shouldImportLibrary")
    override val canImportLibrary = mutableStateFlow(IsValid.valid, debugName = "canImportLibrary")

    override val shouldImportProviderAccounts = viewMutableStateFlow(false, debugName = "shouldImportProviderAccounts")
    override val canImportProviderAccounts = mutableStateFlow(IsValid.valid, debugName = "canImportProviderAccounts")

    override val shouldImportFilters = viewMutableStateFlow(false, debugName = "shouldImportFilters")
    override val canImportFilters = mutableStateFlow(IsValid.valid, debugName = "canImportFilters")

    override val browseActions = broadcastFlow<Unit>()

    init {
        register()
    }

    override val root = borderpane {
        top = confirmationToolbar()
        center {
            form {
                minWidth = 600.0
                fieldset("The existing database will be lost!", Icons.warning)
                fieldset {
                    pathField()
                }
                fieldset("Import") {
                    horizontalField("Library") {
                        jfxCheckBox(shouldImportLibrary.property) {
                            enableWhen(canImportLibrary)
                        }
                    }
                    horizontalField("Provider Accounts") {
                        jfxCheckBox(shouldImportProviderAccounts.property) {
                            enableWhen(canImportProviderAccounts)
                        }
                    }
                    horizontalField("Filters") {
                        jfxCheckBox(shouldImportFilters.property) {
                            enableWhen(canImportFilters)
                        }
                    }
                }
            }
        }
    }

    private fun Fieldset.pathField() = horizontalField("Path") {
        jfxTextField(importDatabaseFile.property, promptText = "Enter Path...") {
            validWhen(importDatabaseFileIsValid)
        }
        browseButton { action(browseActions) }
    }


    override fun selectImportDatabaseFile(initialDirectory: File?) =
        chooseFile("Select Database File...", filters = emptyArray()) {
            this@chooseFile.initialDirectory = initialDirectory
        }.firstOrNull()
}