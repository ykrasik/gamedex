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

package com.gitlab.ykrasik.gamedex.ui.view.settings

import com.gitlab.ykrasik.gamedex.controller.SettingsController
import com.gitlab.ykrasik.gamedex.javafx.jfxButton
import com.gitlab.ykrasik.gamedex.javafx.CommonStyle
import com.gitlab.ykrasik.gamedex.javafx.Theme
import javafx.event.EventTarget
import javafx.geometry.Pos
import javafx.scene.layout.GridPane
import javafx.scene.paint.Color
import javafx.scene.text.FontWeight
import tornadofx.*

/**
 * User: ykrasik
 * Date: 05/06/2017
 * Time: 14:57
 */
class GeneralSettingsView : View("General Settings", Theme.Icon.settings()) {
    private val settingsController: SettingsController by di()

    override val root = vbox {
        group("Database") {
            row {
                jfxButton("Export Database", Theme.Icon.upload()) {
                    addClass(CommonStyle.thinBorder, Style.exportButton)
                    useMaxWidth = true
                    alignment = Pos.CENTER_LEFT
                    enableWhen { settingsController.canRunLongTask }
                    setOnAction { settingsController.exportDatabase() }
                }
            }
            row {
                jfxButton("Import Database", Theme.Icon.download()) {
                    addClass(CommonStyle.thinBorder, Style.importButton)
                    useMaxWidth = true
                    alignment = Pos.CENTER_LEFT
                    enableWhen { settingsController.canRunLongTask }
                    setOnAction { settingsController.importDatabase() }
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
                    enableWhen { settingsController.canRunLongTask }
                    setOnAction { settingsController.clearUserData() }
                }
            }
            row {
                jfxButton("Cleanup", Theme.Icon.delete(color = Color.RED)) {
                    addClass(CommonStyle.thinBorder, Style.cleanupDbButton)
                    useMaxWidth = true
                    alignment = Pos.CENTER_LEFT
                    enableWhen { settingsController.canRunLongTask }
                    setOnAction { settingsController.cleanupDb() }
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