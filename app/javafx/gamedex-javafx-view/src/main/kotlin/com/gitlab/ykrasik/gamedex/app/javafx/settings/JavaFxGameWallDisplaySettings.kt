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

package com.gitlab.ykrasik.gamedex.app.javafx.settings

import com.gitlab.ykrasik.gamedex.app.api.settings.GameWallDisplaySettings
import com.gitlab.ykrasik.gamedex.app.api.settings.ImageDisplayType
import com.gitlab.ykrasik.gamedex.app.api.settings.MutableGameWallDisplaySettings
import com.gitlab.ykrasik.gamedex.javafx.userMutableState
import tornadofx.onChange

/**
 * User: ykrasik
 * Date: 10/06/2018
 * Time: 12:08
 */
class JavaFxGameWallDisplaySettings : MutableGameWallDisplaySettings, GameWallDisplaySettings {
    override val imageDisplayType = userMutableState(ImageDisplayType.Fixed)
    override val showBorder = userMutableState(false)
    override val width = userMutableState(0.0)
    override val height = userMutableState(0.0)
    override val horizontalSpacing = userMutableState(0.0)
    override val verticalSpacing = userMutableState(0.0)

    inline fun onChange(crossinline f: () -> Unit) = listOf(
        imageDisplayType.property, showBorder.property, width.property, height.property, horizontalSpacing.property, verticalSpacing.property
    ).forEach { it.onChange { f() } }
}