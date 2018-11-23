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
import com.gitlab.ykrasik.gamedex.javafx.control.adjustableTextField
import com.gitlab.ykrasik.gamedex.javafx.enumComboMenu
import com.gitlab.ykrasik.gamedex.javafx.jfxToggleButton
import tornadofx.Fragment
import tornadofx.field
import tornadofx.fieldset
import tornadofx.form

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
            field("Width") { adjustableTextField(settings.widthProperty, "cell width", min = 20.0, max = 500.0) }
            field("Height") { adjustableTextField(settings.heightProperty, "cell height", min = 20.0, max = 500.0) }
            field("Horizontal Spacing") { adjustableTextField(settings.horizontalSpacingProperty, "horizontal spacing", min = 0.0, max = 100.0) }
            field("Vertical Spacing") { adjustableTextField(settings.verticalSpacingProperty, "vertical spacing", min = 0.0, max = 100.0) }
        }
    }
}