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
import tornadofx.Fragment
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
            horizontalField("Show") { jfxCheckBox(settings.enabled.property) }
            horizontalField("Only on Mouse Over") {
                showWhen { settings.enabled.property }
                jfxCheckBox(settings.showOnlyWhenActive.property)
            }
        }
        fieldset("Position") {
            showWhen { settings.enabled.property }
            horizontalField("Position") { enumComboMenu(settings.position.property) }
            horizontalField("Fill Width") { jfxCheckBox(settings.fillWidth.property) }
        }
        fieldset("Font") {
            showWhen { settings.enabled.property }
            horizontalField("Size") { plusMinusSlider(settings.fontSize.property, min = 1, max = 90) }
            horizontalField("Bold") { jfxCheckBox(settings.boldFont.property) }
            horizontalField("Italic") { jfxCheckBox(settings.italicFont.property) }
            horizontalField("Color") { jfxColorPicker(settings.textColor.property) }
        }
        fieldset("Background") {
            showWhen { settings.enabled.property }
            horizontalField("Color") { jfxColorPicker(settings.backgroundColor.property) }
            horizontalField("Opacity") { percentSlider(settings.opacity.property) }
        }
    }
}