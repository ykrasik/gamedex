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

package com.gitlab.ykrasik.gamedex.javafx.settings

import com.gitlab.ykrasik.gamedex.core.userconfig.UserConfigRepository
import com.gitlab.ykrasik.gamedex.javafx.*
import com.gitlab.ykrasik.gamedex.javafx.control.adjustableTextField
import com.gitlab.ykrasik.gamedex.javafx.game.wall.GameWallUserConfig
import javafx.beans.property.Property
import javafx.geometry.Pos
import javafx.scene.paint.Color
import tornadofx.*

/**
 * User: ykrasik
 * Date: 05/06/2017
 * Time: 15:21
 */
class GameSettingsView : View("Game Settings", Theme.Icon.games()) {
    private val userConfigRepository: UserConfigRepository by di()
    private val gameWallUserConfig = userConfigRepository[GameWallUserConfig::class]

    override val root = scrollpane {
        form {
            fieldset("Overlay") {
                overlaySettings("Name", gameWallUserConfig.nameOverlay)
                overlaySettings("MetaTag", gameWallUserConfig.metaTagOverlay)
                overlaySettings("Version", gameWallUserConfig.versionOverlay)
            }
            fieldset("Cell") {
                cellSettings()
            }
        }
    }

    private fun Fieldset.overlaySettings(name: String, overlay: GameWallUserConfig.OverlaySettingsAccessor) =
        field("Display $name Overlay") {
            form {
                val isShowProperty = overlay.isShowSubject.toPropertyCached()
                fieldset {
                    field("Show") {
                        hbox(spacing = 5, alignment = Pos.CENTER_LEFT) {
                            jfxToggleButton(isShowProperty)
                            jfxCheckBox(overlay.showOnlyWhenActiveSubject.toPropertyCached(), "Only When Active") {
                                showWhen { isShowProperty }
                            }
                        }
                    }
                }
                fieldset {
                    showWhen { isShowProperty }
                    field("Position") {
                        hbox(spacing = 5, alignment = Pos.CENTER_LEFT) {
                            enumComboBox(overlay.positionSubject.toPropertyCached())
                            jfxCheckBox(overlay.fillWidthSubject.toPropertyCached(), "Fill Width")
                        }
                    }
                    field("Font") {
                        hbox(spacing = 5, alignment = Pos.CENTER_LEFT) {
                            adjustableTextField(overlay.fontSizeSubject.toPropertyCached(), "Font Size", min = 1, max = 90)
                            jfxCheckBox(overlay.boldSubject.toPropertyCached(), "Bold")
                            jfxCheckBox(overlay.italicSubject.toPropertyCached(), "Italic")
                        }
                    }
                    field("Font Color") {
                        colorpicker(Color.valueOf(overlay.fontColor)) {
                            setOnAction {
                                overlay.fontColor = value.toString()
                            }
                        }
                    }
                    field("Background Color") {
                        colorpicker(Color.valueOf(overlay.backgroundColor)) {
                            setOnAction {
                                overlay.backgroundColor = value.toString()
                            }
                        }
                    }
                    field("Opacity") {
                        hbox(spacing = 5, alignment = Pos.CENTER_LEFT) {
                            val slider = slider(min = 0, max = 1.0, value = overlay.opacity) {
                                valueProperty().bindBidirectional(overlay.opacitySubject.toPropertyCached() as Property<Number>)
                            }
                            label(slider.valueProperty().stringBinding { it!!.toString() })
                        }
                    }
                }
            }
        }

    private fun Fieldset.cellSettings() {
        field("Fixed Size") {
            hbox(spacing = 5.0) {
                alignment = Pos.CENTER_LEFT
                jfxToggleButton(gameWallUserConfig.cell.isFixedSizeSubject.toPropertyCached())
                labeled("Thumbnail") { enumComboBox(gameWallUserConfig.cell.imageDisplayTypeSubject.toPropertyCached()) }.apply {
                    showWhen { gameWallUserConfig.cell.isFixedSizeSubject.toPropertyCached() }
                }
            }
        }
        field("Border") { jfxToggleButton(gameWallUserConfig.cell.isShowBorderSubject.toPropertyCached()) }
        field("Width") { adjustableTextField(gameWallUserConfig.cell.widthSubject.toPropertyCached(), "cell width", min = 20.0, max = 500.0) }
        field("Height") { adjustableTextField(gameWallUserConfig.cell.heightSubject.toPropertyCached(), "cell height", min = 20.0, max = 500.0) }
        field("Horizontal Spacing") { adjustableTextField(gameWallUserConfig.cell.horizontalSpacingSubject.toPropertyCached(), "horizontal spacing", min = 0.0, max = 100.0) }
        field("Vertical Spacing") { adjustableTextField(gameWallUserConfig.cell.verticalSpacingSubject.toPropertyCached(), "vertical spacing", min = 0.0, max = 100.0) }
    }

    class Style : Stylesheet() {
        companion object {
            val gameWallSettings by cssid()

            init {
                importStylesheet(Style::class)
            }
        }

        init {
            gameWallSettings {
                maxWidth = 400.px
            }
        }
    }
}