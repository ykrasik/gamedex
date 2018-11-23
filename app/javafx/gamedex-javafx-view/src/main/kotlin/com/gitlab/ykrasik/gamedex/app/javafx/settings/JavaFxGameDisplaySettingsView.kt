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

import com.gitlab.ykrasik.gamedex.javafx.*
import com.gitlab.ykrasik.gamedex.javafx.control.adjustableTextField
import javafx.geometry.Pos
import javafx.scene.paint.Color
import tornadofx.*

/**
 * User: ykrasik
 * Date: 05/06/2017
 * Time: 15:21
 */
class JavaFxGameDisplaySettingsView(settings: JavaFxOverlayDisplaySettings, name: String) : Fragment("$name Display", Icons.display) {
    override val root = form {
        fieldset {
            field("Show") {
                hbox(spacing = 5, alignment = Pos.CENTER_LEFT) {
                    jfxToggleButton(settings.enabledProperty)
                    jfxCheckBox(settings.showOnlyWhenActiveProperty, "Only When Active") {
                        showWhen { settings.enabledProperty }
                    }
                }
            }
        }
        fieldset {
            showWhen { settings.enabledProperty }
            field("Position") {
                hbox(spacing = 5, alignment = Pos.CENTER_LEFT) {
                    enumComboMenu(settings.positionProperty)
                    jfxCheckBox(settings.fillWidthProperty, "Fill Width")
                }
            }
            field("Font") {
                hbox(spacing = 5, alignment = Pos.CENTER_LEFT) {
                    adjustableTextField(settings.fontSizeProperty, "Font Size", min = 1, max = 90)
                    jfxCheckBox(settings.boldFontProperty, "Bold")
                    jfxCheckBox(settings.italicFontProperty, "Italic")
                }
            }
            field("Font Color") {
                colorpicker {
                    settings.textColorProperty.perform {
                        value = Color.valueOf(it)
                    }
                    setOnAction {
                        settings.textColor = value.toString()
                    }
                }
            }
            field("Background Color") {
                colorpicker {
                    settings.backgroundColorProperty.perform {
                        value = Color.valueOf(it)
                    }
                    setOnAction {
                        settings.backgroundColor = value.toString()
                    }
                }
            }
            field("Opacity") { percentSlider(settings.opacityProperty, min = 0, max = 1.0) }
        }
    }
}