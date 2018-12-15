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

import com.gitlab.ykrasik.gamedex.javafx.Icons
import com.gitlab.ykrasik.gamedex.javafx.control.*
import com.gitlab.ykrasik.gamedex.javafx.showWhen
import tornadofx.Fragment
import tornadofx.field
import tornadofx.fieldset
import tornadofx.form

/**
 * User: ykrasik
 * Date: 05/06/2017
 * Time: 15:21
 */
class JavaFxGameDisplaySettingsView(settings: JavaFxOverlayDisplaySettings, name: String) : Fragment("$name Display", Icons.display) {
    override val root = form {
        fieldset("Enabled") {
            field("Show") { jfxCheckBox(settings.enabledProperty) }
            field("Only on Mouse Over") {
                showWhen { settings.enabledProperty }
                jfxCheckBox(settings.showOnlyWhenActiveProperty)
            }
        }
        fieldset("Position") {
            showWhen { settings.enabledProperty }
            field("Position") { enumComboMenu(settings.positionProperty) }
            field("Fill Width") { jfxCheckBox(settings.fillWidthProperty) }
        }
        fieldset("Font") {
            showWhen { settings.enabledProperty }
            field("Size") { plusMinusSlider(settings.fontSizeProperty, min = 1, max = 90) }
            field("Bold") { jfxCheckBox(settings.boldFontProperty) }
            field("Italic") { jfxCheckBox(settings.italicFontProperty) }
            field("Color") { jfxColorPicker(settings.textColorProperty) }
        }
        fieldset("Background") {
            showWhen { settings.enabledProperty }
            field("Color") { jfxColorPicker(settings.backgroundColorProperty) }
            field("Opacity") { percentSlider(settings.opacityProperty) }
        }
    }
}