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
import com.gitlab.ykrasik.gamedex.javafx.control.enumComboMenu
import com.gitlab.ykrasik.gamedex.javafx.control.jfxButton
import com.gitlab.ykrasik.gamedex.javafx.control.jfxToggleButton
import com.gitlab.ykrasik.gamedex.javafx.control.plusMinusSlider
import com.gitlab.ykrasik.gamedex.javafx.defaultHbox
import com.gitlab.ykrasik.gamedex.javafx.size
import tornadofx.*

/**
 * User: ykrasik
 * Date: 29/11/2018
 * Time: 08:20
 */
class JavaFxWallDisplaySettingsView(settings: JavaFxGameWallDisplaySettings) : Fragment("Game Wall", Icons.grid) {
    override val root = form {
        fieldset {
            field("Fixed Size") { enumComboMenu(settings.imageDisplayTypeProperty) }
            field("Border") { jfxToggleButton(settings.showBorderProperty) }
            field("Width") {
                defaultHbox {
                    plusMinusSlider(settings.widthProperty, min = 20, max = 500)
                    jfxButton("Set from Height", Icons.equal.size(20)) {
                        action { settings.width = settings.height }
                    }
                }
            }
            field("Height") {
                defaultHbox {
                    plusMinusSlider(settings.heightProperty, min = 20, max = 500)
                    jfxButton("Set from Width", Icons.equal.size(20)) {
                        action { settings.height = settings.width }
                    }
                }
            }
            field("Horizontal Spacing") { plusMinusSlider(settings.horizontalSpacingProperty, min = 0, max = 100) }
            field("Vertical Spacing") { plusMinusSlider(settings.verticalSpacingProperty, min = 0, max = 100) }
        }
    }
}