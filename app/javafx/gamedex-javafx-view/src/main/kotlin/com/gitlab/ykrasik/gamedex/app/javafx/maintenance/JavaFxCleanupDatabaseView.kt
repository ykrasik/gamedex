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

package com.gitlab.ykrasik.gamedex.app.javafx.maintenance

import com.gitlab.ykrasik.gamedex.app.api.maintenance.CleanupDatabaseView
import com.gitlab.ykrasik.gamedex.app.api.maintenance.StaleData
import com.gitlab.ykrasik.gamedex.app.api.maintenance.StaleDataCategory
import com.gitlab.ykrasik.gamedex.app.api.util.IsValid
import com.gitlab.ykrasik.gamedex.app.api.util.channel
import com.gitlab.ykrasik.gamedex.javafx.*
import com.gitlab.ykrasik.gamedex.javafx.control.buttonWithPopover
import com.gitlab.ykrasik.gamedex.javafx.control.enableWhen
import com.gitlab.ykrasik.gamedex.javafx.control.jfxCheckBox
import com.gitlab.ykrasik.gamedex.javafx.control.jfxListView
import com.gitlab.ykrasik.gamedex.javafx.view.PresentableView
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.ObservableValue
import javafx.event.EventTarget
import javafx.scene.control.ScrollPane
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import org.controlsfx.control.PopOver
import tornadofx.*

/**
 * User: ykrasik
 * Date: 16/12/2018
 * Time: 09:57
 */
class JavaFxCleanupDatabaseView : PresentableView("Cleanup Database", Icons.databaseCleanup), CleanupDatabaseView {
    val staleDataProperty = SimpleObjectProperty(StaleData(emptyList(), emptyList(), emptyMap(), emptyMap()))
    override var staleData by staleDataProperty

    override val librariesAndGames = JavaFxStaleDataCategory()
    override val images = JavaFxStaleDataCategory()
    override val fileCache = JavaFxStaleDataCategory()

    private val canAcceptProperty = SimpleObjectProperty(IsValid.valid)
    override var canAccept by canAcceptProperty

    override val acceptActions = channel<Unit>()
    override val cancelActions = channel<Unit>()

    init {
        viewRegistry.onCreate(this)
    }

    override val root = borderpane {
        prefWidth = 600.0
        minHeight = 300.0
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
        center = form {
            paddingAll = 10
            fieldset("Select stale data to delete") {
                field("Libraries & Games") {
                    label.graphic = Icons.hdd.color(Color.BLACK)
                    showWhen { librariesAndGames.canDeleteProperty.booleanBinding { it!!.isSuccess } }
                    jfxCheckBox(librariesAndGames.shouldDeleteProperty)

                    viewButton(staleDataProperty.stringBinding { "${it!!.libraries.size} Libraries" }) {
                        jfxListView(staleDataProperty.mapToList { it.libraries.map { it.path } })
                    }.apply {
                        showWhen { staleDataProperty.booleanBinding { it!!.libraries.isNotEmpty() } }
                    }

                    viewButton(staleDataProperty.stringBinding { "${it!!.games.size} Games" }) {
                        jfxListView(staleDataProperty.mapToList { it.games.map { it.path } })
                    }.apply {
                        showWhen { staleDataProperty.booleanBinding { it!!.games.isNotEmpty() } }
                    }
                }
                field("Images") {
                    label.graphic = Icons.thumbnail
                    showWhen { images.canDeleteProperty.booleanBinding { it!!.isSuccess } }
                    jfxCheckBox(images.shouldDeleteProperty)
                    label(staleDataProperty.stringBinding { "${it!!.images.size} Images: ${it.staleImagesSizeTaken.humanReadable}" })
                }
                field("File Cache") {
                    label.graphic = Icons.fileQuestion
                    showWhen { fileCache.canDeleteProperty.booleanBinding { it!!.isSuccess } }
                    jfxCheckBox(fileCache.shouldDeleteProperty)
                    label(staleDataProperty.stringBinding { "${it!!.fileStructure.size} File Cache Entries: ${it.staleFileStructureSizeTaken.humanReadable}" })
                }
            }
        }
    }

    private inline fun EventTarget.viewButton(textProperty: ObservableValue<String>, crossinline op: VBox.() -> Unit = {}) =
        buttonWithPopover("", Icons.details, PopOver.ArrowLocation.LEFT_CENTER) { popOver ->
            (popOver.contentNode as ScrollPane).minWidth = 600.0
            op()
        }.apply {
            addClass(CommonStyle.infoButton)
            textProperty().bind(textProperty)
        }

    class JavaFxStaleDataCategory : StaleDataCategory {
        val canDeleteProperty = SimpleObjectProperty(IsValid.valid)
        override var canDelete by canDeleteProperty

        private var ignoreNextChange = false

        override val shouldDeleteChanges = channel<Boolean>()
        val shouldDeleteProperty = SimpleBooleanProperty(false).apply {
            onChange {
                if (!ignoreNextChange) {
                    shouldDeleteChanges.offer(it)
                }
            }
        }
        override var shouldDelete: Boolean
            get() = shouldDeleteProperty.value
            set(value) {
                ignoreNextChange = true
                shouldDeleteProperty.value = value
                ignoreNextChange = false
            }
    }
}