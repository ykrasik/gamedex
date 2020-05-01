/****************************************************************************
 * Copyright (C) 2016-2020 Yevgeny Krasik                                   *
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

import com.gitlab.ykrasik.gamedex.app.api.settings.DisplayPosition
import com.gitlab.ykrasik.gamedex.app.api.settings.MutableOverlayDisplaySettings
import com.gitlab.ykrasik.gamedex.app.api.settings.OverlayDisplaySettings
import com.gitlab.ykrasik.gamedex.javafx.userMutableState
import tornadofx.onChange

/**
 * User: ykrasik
 * Date: 10/06/2018
 * Time: 14:29
 */
class JavaFxOverlayDisplaySettings : MutableOverlayDisplaySettings, OverlayDisplaySettings {
    override val enabled = userMutableState(false)
    override val showOnlyWhenActive = userMutableState(false)
    override val position = userMutableState(DisplayPosition.Center)
    override val fillWidth = userMutableState(false)
    override val fontSize = userMutableState(0)
    override val boldFont = userMutableState(false)
    override val italicFont = userMutableState(false)
    override val textColor = userMutableState("#000000")
    override val backgroundColor = userMutableState("#000000")
    override val opacity = userMutableState(0.0)

    inline fun onChange(crossinline f: () -> Unit) = listOf(
        enabled.property, showOnlyWhenActive.property, position.property, fillWidth.property, fontSize.property, boldFont.property,
        italicFont.property, textColor.property, backgroundColor.property, opacity.property
    ).forEach { it.onChange { f() } }
}