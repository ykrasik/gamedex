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
import com.gitlab.ykrasik.gamedex.javafx.mutableStateFlow
import com.gitlab.ykrasik.gamedex.javafx.viewMutableStateFlow
import kotlinx.coroutines.flow.combine

/**
 * User: ykrasik
 * Date: 10/06/2018
 * Time: 14:29
 */
class JavaFxOverlayDisplaySettings : OverlayDisplaySettings {
    override val enabled = mutableStateFlow(false, debugName = "enabled")
    override val showOnlyWhenActive = mutableStateFlow(false, debugName = "showOnlyWhenActive")
    override val position = mutableStateFlow(DisplayPosition.Center, debugName = "position")
    override val fillWidth = mutableStateFlow(false, debugName = "fillWidth")
    override val fontSize = mutableStateFlow(1, debugName = "fontSize")
    override val boldFont = mutableStateFlow(false, debugName = "boldFont")
    override val italicFont = mutableStateFlow(false, debugName = "italicFont")
    override val textColor = mutableStateFlow("#000000", debugName = "textColor")
    override val backgroundColor = mutableStateFlow("#000000", debugName = "backgroundColor")
    override val opacity = mutableStateFlow(0.0, debugName = "opacity")

    fun changes() = combine(enabled, showOnlyWhenActive, position, fillWidth, fontSize, boldFont, italicFont, textColor, backgroundColor, opacity) { }
}

class JavaFxMutableOverlayDisplaySettings(debugName: String) : MutableOverlayDisplaySettings {
    override val enabled = viewMutableStateFlow(false, debugName = "$debugName.enabled")
    override val showOnlyWhenActive = viewMutableStateFlow(false, debugName = "$debugName.showOnlyWhenActive")
    override val position = viewMutableStateFlow(DisplayPosition.Center, debugName = "$debugName.position")
    override val fillWidth = viewMutableStateFlow(false, debugName = "$debugName.fillWidth")
    override val fontSize = viewMutableStateFlow(1, debugName = "$debugName.fontSize")
    override val boldFont = viewMutableStateFlow(false, debugName = "$debugName.boldFont")
    override val italicFont = viewMutableStateFlow(false, debugName = "$debugName.italicFont")
    override val textColor = viewMutableStateFlow("#000000", debugName = "$debugName.textColor")
    override val backgroundColor = viewMutableStateFlow("#000000", debugName = "$debugName.backgroundColor")
    override val opacity = viewMutableStateFlow(0.0, debugName = "$debugName.opacity")
}