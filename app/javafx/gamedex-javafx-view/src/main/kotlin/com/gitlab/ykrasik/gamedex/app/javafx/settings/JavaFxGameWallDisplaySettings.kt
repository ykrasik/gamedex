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

import com.gitlab.ykrasik.gamedex.app.api.settings.GameWallDisplaySettings
import com.gitlab.ykrasik.gamedex.app.api.settings.ImageDisplayType
import com.gitlab.ykrasik.gamedex.app.api.settings.MutableGameWallDisplaySettings
import com.gitlab.ykrasik.gamedex.javafx.mutableStateFlow
import com.gitlab.ykrasik.gamedex.javafx.viewMutableStateFlow
import kotlinx.coroutines.flow.combine

/**
 * User: ykrasik
 * Date: 10/06/2018
 * Time: 12:08
 */
class JavaFxGameWallDisplaySettings : GameWallDisplaySettings {
    override val imageDisplayType = mutableStateFlow(ImageDisplayType.Fixed, debugName = "imageDisplayType")
    override val showBorder = mutableStateFlow(false, debugName = "showBorder")
    override val width = mutableStateFlow(1.0, debugName = "width")
    override val height = mutableStateFlow(1.0, debugName = "height")
    override val horizontalSpacing = mutableStateFlow(1.0, debugName = "horizontalSpacing")
    override val verticalSpacing = mutableStateFlow(1.0, debugName = "verticalSpacing")

    fun changes() = combine(imageDisplayType, showBorder, width, height, horizontalSpacing, verticalSpacing) { }
}

class JavaFxMutableGameWallDisplaySettings : MutableGameWallDisplaySettings {
    override val imageDisplayType = viewMutableStateFlow(ImageDisplayType.Fixed, debugName = "imageDisplayType")
    override val showBorder = viewMutableStateFlow(false, debugName = "showBorder")
    override val width = viewMutableStateFlow(1.0, debugName = "width")
    override val height = viewMutableStateFlow(1.0, debugName = "height")
    override val horizontalSpacing = viewMutableStateFlow(1.0, debugName = "horizontalSpacing")
    override val verticalSpacing = viewMutableStateFlow(1.0, debugName = "verticalSpacing")
}