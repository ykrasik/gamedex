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

package com.gitlab.ykrasik.gamedex.app.javafx.maintenance

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.LibraryPath
import com.gitlab.ykrasik.gamedex.app.api.maintenance.CleanupData
import com.gitlab.ykrasik.gamedex.app.api.maintenance.CleanupDatabaseView
import com.gitlab.ykrasik.gamedex.javafx.*
import com.gitlab.ykrasik.gamedex.javafx.control.*
import com.gitlab.ykrasik.gamedex.javafx.theme.GameDexStyle
import com.gitlab.ykrasik.gamedex.javafx.theme.Icons
import com.gitlab.ykrasik.gamedex.javafx.theme.color
import com.gitlab.ykrasik.gamedex.javafx.view.ConfirmationWindow
import com.jfoenix.controls.JFXListCell
import javafx.beans.value.ObservableValue
import javafx.event.EventTarget
import javafx.geometry.Pos
import javafx.scene.control.ScrollPane
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import tornadofx.*

/**
 * User: ykrasik
 * Date: 16/12/2018
 * Time: 09:57
 */
class JavaFxCleanupDatabaseView : ConfirmationWindow("Cleanup Database", Icons.databaseCleanup), CleanupDatabaseView {
    override val cleanupData = viewMutableStateFlow(CleanupData.Null, debugName = "cleanupData")

    override val movedGamesToFix = viewMutableStateFlow(emptyList<Pair<Game, LibraryPath>>(), debugName = "movedGamesToFix")
    override val isDeleteLibrariesAndGames = viewMutableStateFlow(false, debugName = "isDeleteLibrariesAndGames")
    override val isDeleteImages = viewMutableStateFlow(false, debugName = "isDeleteImages")
    override val isDeleteFileCache = viewMutableStateFlow(false, debugName = "isDeleteFileCache")

    init {
        register()
    }

    override val root = borderpane {
        prefWidth = 1000.0
        minHeight = 300.0
        top = confirmationToolbar()
        center = form {
            paddingAll = 10
            fieldset("Select cleanup to perform") {
                horizontalField("Fix Moved Games") {
                    label.graphic = Icons.fix
                    showWhen { cleanupData.property.typesafeBooleanBinding { it.movedGames.isNotEmpty() } }
                    prettyListView(cleanupData.property.mapToList { it.movedGames }) {
                        setCellFactory {
                            object : JFXListCell<Pair<Game, LibraryPath>>() {

                                override fun updateItem(data: Pair<Game, LibraryPath>?, empty: Boolean) {
                                    super.updateItem(data, empty)
                                    if (data == null) return

                                    text = null
                                    graphic = HBox().apply {
                                        alignment = Pos.CENTER_LEFT
                                        spacing = 10.0

                                        jfxCheckBox {
                                            isSelected = true
                                            selectedProperty().onChange { selected ->
                                                movedGamesToFix.valueFromView = if (selected) {
                                                    (movedGamesToFix.v + data).distinct()
                                                } else {
                                                    movedGamesToFix.v.filter { toFix -> toFix != data }
                                                }
                                            }
                                        }

                                        label(data.first.path.path) {
//                                            maxWidth = 270.0
//                                            isWrapText = true
                                        }
                                        label(graphic = Icons.arrowRightBold)
                                        label(data.second.path.path) {

                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                horizontalField("Delete Stale Libraries & Games") {
                    label.graphic = Icons.hdd.color(Color.BLACK)
                    showWhen { cleanupData.property.map { it.missingLibraries.isNotEmpty() || it.missingGames.isNotEmpty() } }
                    jfxCheckBox(isDeleteLibrariesAndGames.property)

                    viewButton(cleanupData.property.typesafeStringBinding { "${it.missingLibraries.size} Libraries" }) {
                        prettyListView(cleanupData.property.mapToList { it.missingLibraries.map { it.path } })
                    }.apply {
                        showWhen { cleanupData.property.typesafeBooleanBinding { it.missingLibraries.isNotEmpty() } }
                    }

                    viewButton(cleanupData.property.typesafeStringBinding { "${it.missingGames.size} Games" }) {
                        prettyListView(cleanupData.property.mapToList { it.missingGames.map { it.path } })
                    }.apply {
                        showWhen { cleanupData.property.typesafeBooleanBinding { it.missingGames.isNotEmpty() } }
                    }
                }
                horizontalField("Delete Stale Images") {
                    label.graphic = Icons.thumbnail
                    showWhen { cleanupData.property.map { it.staleImages.isNotEmpty() } }
                    jfxCheckBox(isDeleteImages.property)

                    viewButton(cleanupData.property.typesafeStringBinding { "${it.staleImages.size} Images: ${it.staleImagesSizeTaken.humanReadable}" }) {
                        prettyListView(cleanupData.property.mapToList { it.staleImages.map { "${it.key} [${it.value}]" } })
                    }.apply {
                        showWhen { cleanupData.property.typesafeBooleanBinding { it.staleImages.isNotEmpty() } }
                    }
                }
                horizontalField("Delete Stale File Cache") {
                    label.graphic = Icons.fileQuestion
                    showWhen { cleanupData.property.map { it.staleFileTrees.isNotEmpty() } }
                    jfxCheckBox(isDeleteFileCache.property)

                    viewButton(cleanupData.property.typesafeStringBinding { "${it.staleFileTrees.size} File Cache Entries: ${it.staleFileTreesSizeTaken.humanReadable}" }) {
                        prettyListView(cleanupData.property.mapToList { it.staleFileTrees.map { "${it.key} [${it.value}]" } })
                    }.apply {
                        showWhen { cleanupData.property.typesafeBooleanBinding { it.staleFileTrees.isNotEmpty() } }
                    }
                }
            }
        }
    }

    private inline fun EventTarget.viewButton(textProperty: ObservableValue<String>, crossinline op: VBox.() -> Unit = {}) =
        buttonWithPopover("", Icons.details) {
            (popOver.contentNode as ScrollPane).minWidth = 600.0
            op()
        }.apply {
            addClass(GameDexStyle.infoButton)
            textProperty().bind(textProperty)
        }
}