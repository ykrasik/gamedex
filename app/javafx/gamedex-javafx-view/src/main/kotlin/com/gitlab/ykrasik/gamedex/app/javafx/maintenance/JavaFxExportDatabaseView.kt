/****************************************************************************
 * Copyright (C) 2016-2019 Yevgeny Krasik                                   *
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

import com.gitlab.ykrasik.gamedex.app.api.file.ViewCanOpenFile
import com.gitlab.ykrasik.gamedex.app.api.maintenance.ExportDatabaseView
import com.gitlab.ykrasik.gamedex.app.api.util.channel
import com.gitlab.ykrasik.gamedex.javafx.control.*
import com.gitlab.ykrasik.gamedex.javafx.state
import com.gitlab.ykrasik.gamedex.javafx.theme.Icons
import com.gitlab.ykrasik.gamedex.javafx.theme.size
import com.gitlab.ykrasik.gamedex.javafx.userMutableState
import com.gitlab.ykrasik.gamedex.javafx.view.ConfirmationWindow
import com.gitlab.ykrasik.gamedex.util.IsValid
import tornadofx.*
import java.io.File

/**
 * User: ykrasik
 * Date: 06/05/2019
 * Time: 09:16
 */
// FIXME: The order in which the view implements interfaces matters, it's a hack -
// FIXME: Since we call browse onShow which in turn calls onAccept, any views that come after ExportDatabaseView
// FIXME: Will not have their onShow called.
class JavaFxExportDatabaseView : ConfirmationWindow("Export Database", Icons.export), ViewCanOpenFile, ExportDatabaseView {
    override val exportDatabaseDirectory = userMutableState("")
    override val exportDatabaseFolderIsValid = state(IsValid.valid)

    override val shouldExportLibrary = userMutableState(false)
    override val shouldExportProviderAccounts = userMutableState(false)
    override val shouldExportFilters = userMutableState(false)

    override val browseActions = channel<Unit>()

    override val openFileActions = channel<File>()

    init {
        register()
    }

    override val root = borderpane {
        top = confirmationToolbar()
        center {
            form {
                minWidth = 600.0
                fieldset {
                    horizontalField("Path") {
                        jfxTextField(exportDatabaseDirectory.property, promptText = "Enter Path...") {
                            validWhen(exportDatabaseFolderIsValid)
                        }
                        jfxButton("Browse", Icons.folderOpen.size(24)) {
                            action(browseActions)
                        }
                    }
                }
                fieldset("Export") {
                    horizontalField("Library") {
                        jfxCheckBox(shouldExportLibrary.property)
                    }
                    horizontalField("Provider Accounts") {
                        jfxCheckBox(shouldExportProviderAccounts.property)
                    }
                    horizontalField("Filters") {
                        jfxCheckBox(shouldExportFilters.property)
                    }
                }
            }
        }
    }


    override fun selectExportDatabaseDirectory(initialDirectory: File?) =
        chooseDirectory("Select Database Export Folder...", initialDirectory)

    override fun openDirectory(directory: File) {
        openFileActions.offer(directory)
    }
}