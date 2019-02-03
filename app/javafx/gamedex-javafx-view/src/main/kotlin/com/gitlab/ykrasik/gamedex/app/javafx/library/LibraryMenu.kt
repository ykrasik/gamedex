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

package com.gitlab.ykrasik.gamedex.app.javafx.library

import com.gitlab.ykrasik.gamedex.Library
import com.gitlab.ykrasik.gamedex.LibraryType
import com.gitlab.ykrasik.gamedex.app.api.file.ViewCanBrowsePath
import com.gitlab.ykrasik.gamedex.app.api.library.ViewCanAddLibrary
import com.gitlab.ykrasik.gamedex.app.api.library.ViewCanDeleteLibrary
import com.gitlab.ykrasik.gamedex.app.api.library.ViewCanEditLibrary
import com.gitlab.ykrasik.gamedex.app.api.library.ViewWithLibraries
import com.gitlab.ykrasik.gamedex.app.api.provider.ViewCanSyncLibraries
import com.gitlab.ykrasik.gamedex.app.api.util.channel
import com.gitlab.ykrasik.gamedex.javafx.control.enableWhen
import com.gitlab.ykrasik.gamedex.javafx.control.jfxButton
import com.gitlab.ykrasik.gamedex.javafx.control.verticalGap
import com.gitlab.ykrasik.gamedex.javafx.perform
import com.gitlab.ykrasik.gamedex.javafx.state
import com.gitlab.ykrasik.gamedex.javafx.theme.*
import com.gitlab.ykrasik.gamedex.javafx.view.PresentableView
import com.gitlab.ykrasik.gamedex.util.IsValid
import javafx.geometry.Pos
import tornadofx.*
import java.io.File

/**
 * User: ykrasik
 * Date: 12/01/2019
 * Time: 21:34
 */
class LibraryMenu : PresentableView("Libraries", Icons.folders),
    ViewCanSyncLibraries,
    ViewWithLibraries,
    ViewCanAddLibrary,
    ViewCanEditLibrary,
    ViewCanDeleteLibrary,
    ViewCanBrowsePath {

    override val canSyncLibraries = state(IsValid.valid)
    override val syncLibrariesActions = channel<Unit>()

    override val libraries = mutableListOf<Library>().observable()

    override val canAddLibraries = state(IsValid.valid)
    override val addLibraryActions = channel<Unit>()

    override val canEditLibraries = state(IsValid.valid)
    override val editLibraryActions = channel<Library>()

    override val canDeleteLibraries = state(IsValid.valid)
    override val deleteLibraryActions = channel<Library>()

    override val browsePathActions = channel<File>()

    init {
        register()
    }

    override val root = vbox(spacing = 5) {
        hbox {
            infoButton("Sync Libraries", graphic = Icons.folderSync) {
                tooltip("Scan all libraries for new games and sync them with providers")
                enableWhen(canSyncLibraries)
                action(syncLibrariesActions)
            }
            spacer()
            addButton {
                alignment = Pos.CENTER
                removeClass(CommonStyle.toolbarButton)
                tooltip("Add a new library")
                enableWhen(canAddLibraries)
                action(addLibraryActions)
            }
        }
        verticalGap()
        vbox {
            libraries.perform { libraries ->
                replaceChildren {
                    gridpane {
                        hgap = 5.0
                        vgap = 3.0
                        libraries.forEach { library ->
                            row {
                                children += if (library.type == LibraryType.Excluded) library.type.icon else library.platform.logo
                                text(library.name)
                                jfxButton(library.path.toString()) {
                                    useMaxWidth = true
                                    addClass(CommonStyle.toolbarButton)
                                    action(browsePathActions) { library.path }
                                }
                                editButton {
                                    removeClass(CommonStyle.toolbarButton)
                                    enableWhen(canEditLibraries)
                                    tooltip("Edit library '${library.name}'")
                                    action(editLibraryActions) { library }
                                }
                                deleteButton {
                                    removeClass(CommonStyle.toolbarButton)
                                    enableWhen(canDeleteLibraries)
                                    tooltip("Delete library '${library.name}'")
                                    action(deleteLibraryActions) { library }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}