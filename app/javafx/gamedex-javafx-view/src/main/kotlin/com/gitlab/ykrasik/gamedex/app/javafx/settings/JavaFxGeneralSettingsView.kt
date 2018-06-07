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

package com.gitlab.ykrasik.gamedex.app.javafx.settings

import com.gitlab.ykrasik.gamedex.app.api.general.*
import com.gitlab.ykrasik.gamedex.app.api.util.BroadcastEventChannel
import com.gitlab.ykrasik.gamedex.app.api.util.channel
import com.gitlab.ykrasik.gamedex.javafx.CommonStyle
import com.gitlab.ykrasik.gamedex.javafx.Theme
import com.gitlab.ykrasik.gamedex.javafx.dialog.areYouSureDialog
import com.gitlab.ykrasik.gamedex.javafx.fitAtMost
import com.gitlab.ykrasik.gamedex.javafx.jfxButton
import com.gitlab.ykrasik.gamedex.javafx.screen.PresentableView
import com.gitlab.ykrasik.gamedex.util.browse
import javafx.event.EventTarget
import javafx.geometry.Pos
import javafx.scene.layout.GridPane
import javafx.scene.paint.Color
import javafx.scene.text.FontWeight
import tornadofx.*
import java.io.File

/**
 * User: ykrasik
 * Date: 05/06/2017
 * Time: 14:57
 */
class JavaFxGeneralSettingsView : PresentableView("General Settings", Theme.Icon.settings()),
    ExportDatabaseView, ImportDatabaseView, ClearUserDataView, CleanupDbView {

    override val exportDatabaseActions = channel<Unit>()
    override val importDatabaseActions = channel<Unit>()
    override val clearUserDataActions = channel<Unit>()
    override val cleanupDbActions = channel<Unit>()

    init {
        viewRegistry.register(this)
    }

    override val root = vbox {
        group("Database") {
            enableWhen { enabledProperty }
            row {
                jfxButton("Export Database", Theme.Icon.upload()) {
                    addClass(CommonStyle.thinBorder, Style.exportButton)
                    useMaxWidth = true
                    alignment = Pos.CENTER_LEFT
                    eventOnAction(exportDatabaseActions)
                }
            }
            row {
                jfxButton("Import Database", Theme.Icon.download()) {
                    addClass(CommonStyle.thinBorder, Style.importButton)
                    useMaxWidth = true
                    alignment = Pos.CENTER_LEFT
                    eventOnAction(importDatabaseActions)
                }
            }
            row {
                region { prefHeight = 40.0 }
            }
            row {
                jfxButton("Clear User Data", Theme.Icon.delete(color = Color.RED)) {
                    addClass(CommonStyle.thinBorder, Style.cleanupDbButton)
                    useMaxWidth = true
                    alignment = Pos.CENTER_LEFT
                    tooltip("Clear game user data, like tags, excluded providers or custom thumbnails for all games.")
                    eventOnAction(clearUserDataActions)
                }
            }
            row {
                jfxButton("Cleanup", Theme.Icon.delete(color = Color.RED)) {
                    addClass(CommonStyle.thinBorder, Style.cleanupDbButton)
                    useMaxWidth = true
                    alignment = Pos.CENTER_LEFT
                    eventOnAction(cleanupDbActions)
                }
            }
        }
    }

    private fun EventTarget.group(title: String, op: GridPane.() -> Unit = {}): GridPane {
        label(title) { addClass(Style.title) }
        return gridpane {
            paddingTop = 5.0
            vgap = 5.0
            op()
        }
    }

    override fun selectDatabaseExportDirectory(initialDirectory: File?) =
        chooseDirectory("Select Database Export Folder...", initialDirectory)

    override fun selectDatabaseImportFile(initialDirectory: File?) =
        chooseFile("Select Database File...", filters = emptyArray()) {
            this@chooseFile.initialDirectory = initialDirectory
        }.firstOrNull()

    override fun browseDirectory(directory: File) = browse(directory)

    override fun confirmImportDatabase() = areYouSureDialog("This will overwrite the existing database.")

    override fun confirmClearUserData() = areYouSureDialog("Clear game user data?") {
        text("This will remove tags, excluded providers & any custom information entered (like custom names or thumbnails) from all games.") {
            wrappingWidth = 400.0
        }
    }

    override fun confirmDeleteStaleData(staleData: StaleData) = areYouSureDialog("Delete the following stale data?") {
        if (staleData.libraries.isNotEmpty()) {
            label("Stale Libraries: ${staleData.libraries.size}")
            listview(staleData.libraries.map { it.path }.observable()) { fitAtMost(5) }
        }
        if (staleData.games.isNotEmpty()) {
            label("Stale Games: ${staleData.games.size}")
            listview(staleData.games.map { it.path }.observable()) { fitAtMost(5) }
        }
        if (staleData.images.isNotEmpty()) {
            label("Stale Images: ${staleData.images.size} (${staleData.staleImagesSize})")
            listview(staleData.images.map { it.first }.observable()) { fitAtMost(5) }
        }
    }

    class Style : Stylesheet() {
        companion object {
            val title by cssclass()
            val importButton by cssclass()
            val exportButton by cssclass()
            val cleanupDbButton by cssclass()

            init {
                importStylesheet(Style::class)
            }
        }

        init {
            title {
                fontSize = 14.px
                fontWeight = FontWeight.BOLD
            }

            importButton {
                and(hover) {
                    backgroundColor = multi(Color.CORNFLOWERBLUE)
                }
            }

            exportButton {
                and(hover) {
                    backgroundColor = multi(Color.LIMEGREEN)
                }
            }

            cleanupDbButton {
                and(hover) {
                    backgroundColor = multi(Color.INDIANRED)
                }
            }
        }
    }
}