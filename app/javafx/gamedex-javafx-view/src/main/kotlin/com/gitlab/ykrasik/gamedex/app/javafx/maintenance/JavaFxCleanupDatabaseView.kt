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

import com.gitlab.ykrasik.gamedex.app.api.maintenance.CleanupDatabaseView
import com.gitlab.ykrasik.gamedex.app.api.maintenance.StaleData
import com.gitlab.ykrasik.gamedex.app.api.maintenance.StaleDataCategory
import com.gitlab.ykrasik.gamedex.javafx.control.*
import com.gitlab.ykrasik.gamedex.javafx.mapToList
import com.gitlab.ykrasik.gamedex.javafx.state
import com.gitlab.ykrasik.gamedex.javafx.theme.CommonStyle
import com.gitlab.ykrasik.gamedex.javafx.theme.Icons
import com.gitlab.ykrasik.gamedex.javafx.theme.color
import com.gitlab.ykrasik.gamedex.javafx.userMutableState
import com.gitlab.ykrasik.gamedex.javafx.view.ConfirmationWindow
import com.gitlab.ykrasik.gamedex.util.IsValid
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.ObservableValue
import javafx.event.EventTarget
import javafx.scene.control.ScrollPane
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import tornadofx.*

/**
 * User: ykrasik
 * Date: 16/12/2018
 * Time: 09:57
 */
class JavaFxCleanupDatabaseView : ConfirmationWindow("Cleanup Database", Icons.databaseCleanup), CleanupDatabaseView {
    val staleDataProperty = SimpleObjectProperty(StaleData(emptyList(), emptyList(), emptyMap(), emptyMap()))
    override var staleData by staleDataProperty

    override val librariesAndGames = JavaFxStaleDataCategory()
    override val images = JavaFxStaleDataCategory()
    override val fileCache = JavaFxStaleDataCategory()

    init {
        register()
    }

    override val root = borderpane {
        prefWidth = 600.0
        minHeight = 300.0
        top = confirmationToolbar()
        center = form {
            paddingAll = 10
            fieldset("Select stale data to delete") {
                horizontalField("Libraries & Games") {
                    label.graphic = Icons.hdd.color(Color.BLACK)
                    showWhen(librariesAndGames.canDelete)
                    jfxCheckBox(librariesAndGames.shouldDelete.property)

                    viewButton(staleDataProperty.stringBinding { "${it!!.libraries.size} Libraries" }) {
                        prettyListView(staleDataProperty.mapToList { it.libraries.map { it.path } })
                    }.apply {
                        showWhen { staleDataProperty.booleanBinding { it!!.libraries.isNotEmpty() } }
                    }

                    viewButton(staleDataProperty.stringBinding { "${it!!.games.size} Games" }) {
                        prettyListView(staleDataProperty.mapToList { it.games.map { it.path } })
                    }.apply {
                        showWhen { staleDataProperty.booleanBinding { it!!.games.isNotEmpty() } }
                    }
                }
                horizontalField("Images") {
                    label.graphic = Icons.thumbnail
                    showWhen(images.canDelete)
                    jfxCheckBox(images.shouldDelete.property)
                    label(staleDataProperty.stringBinding { "${it!!.images.size} Images: ${it.staleImagesSizeTaken.humanReadable}" })
                }
                horizontalField("File Cache") {
                    label.graphic = Icons.fileQuestion
                    showWhen(fileCache.canDelete)
                    jfxCheckBox(fileCache.shouldDelete.property)
                    label(staleDataProperty.stringBinding { "${it!!.fileTrees.size} File Cache Entries: ${it.staleFileTreesSizeTaken.humanReadable}" })
                }
            }
        }
    }

    private inline fun EventTarget.viewButton(textProperty: ObservableValue<String>, crossinline op: VBox.() -> Unit = {}) =
        buttonWithPopover("", Icons.details) {
            (popOver.contentNode as ScrollPane).minWidth = 600.0
            op()
        }.apply {
            addClass(CommonStyle.infoButton)
            textProperty().bind(textProperty)
        }

    class JavaFxStaleDataCategory : StaleDataCategory {
        override val canDelete = state(IsValid.valid)
        override val shouldDelete = userMutableState(false)
    }
}