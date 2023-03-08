/****************************************************************************
 * Copyright (C) 2016-2023 Yevgeny Krasik                                   *
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

import com.gitlab.ykrasik.gamedex.app.api.settings.ImageDisplayType
import com.gitlab.ykrasik.gamedex.javafx.control.*
import com.gitlab.ykrasik.gamedex.javafx.theme.Icons
import com.gitlab.ykrasik.gamedex.javafx.theme.size
import tornadofx.Fragment
import tornadofx.action
import tornadofx.fieldset
import tornadofx.form

/**
 * User: ykrasik
 * Date: 29/11/2018
 * Time: 08:20
 */
class JavaFxWallDisplaySettingsView(settings: JavaFxMutableGameWallDisplaySettings) : Fragment("Game Wall", Icons.grid) {
    override val root = form {
        fieldset {
            horizontalField("Cell Size") { enumComboMenu(settings.imageDisplayType.property, text = ImageDisplayType::displayName) }
            horizontalField("Border") { jfxToggleButton(settings.showBorder.property) }
            horizontalField("Width") {
                defaultHbox {
                    plusMinusSlider(settings.width.property, min = 20, max = 500)
                    jfxButton("Set from Height", Icons.equal.size(20)) {
                        action { settings.width.value = settings.height.value }
                    }
                }
            }
            horizontalField("Height") {
                defaultHbox {
                    plusMinusSlider(settings.height.property, min = 20, max = 500)
                    jfxButton("Set from Width", Icons.equal.size(20)) {
                        action { settings.height.value = settings.width.value }
                    }
                }
            }
            horizontalField("Horizontal Spacing") { plusMinusSlider(settings.horizontalSpacing.property, min = 0, max = 100) }
            horizontalField("Vertical Spacing") { plusMinusSlider(settings.verticalSpacing.property, min = 0, max = 100) }
        }
    }
}