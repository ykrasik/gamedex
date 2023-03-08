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

package com.gitlab.ykrasik.gamedex.app.api.settings

import com.gitlab.ykrasik.gamedex.app.api.util.ViewMutableStateFlow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * User: ykrasik
 * Date: 10/06/2018
 * Time: 14:21
 */
interface ViewWithNameOverlayDisplaySettings {
    val nameOverlayDisplaySettings: OverlayDisplaySettings
}

interface ViewCanChangeNameOverlayDisplaySettings {
    val mutableNameOverlayDisplaySettings: MutableOverlayDisplaySettings
}

interface ViewWithMetaTagOverlayDisplaySettings {
    val metaTagOverlayDisplaySettings: OverlayDisplaySettings
}

interface ViewCanChangeMetaTagOverlayDisplaySettings {
    val mutableMetaTagOverlayDisplaySettings: MutableOverlayDisplaySettings
}

interface ViewCanChangeVersionOverlayDisplaySettings {
    val mutableVersionOverlayDisplaySettings: MutableOverlayDisplaySettings
}

interface ViewWithVersionOverlayDisplaySettings {
    val versionOverlayDisplaySettings: OverlayDisplaySettings
}

interface OverlayDisplaySettings {
    val enabled: MutableStateFlow<Boolean>
    val showOnlyWhenActive: MutableStateFlow<Boolean>
    val position: MutableStateFlow<DisplayPosition>
    val fillWidth: MutableStateFlow<Boolean>
    val fontSize: MutableStateFlow<Int>
    val boldFont: MutableStateFlow<Boolean>
    val italicFont: MutableStateFlow<Boolean>
    val textColor: MutableStateFlow<String>
    val backgroundColor: MutableStateFlow<String>
    val opacity: MutableStateFlow<Double>
}

interface MutableOverlayDisplaySettings {
    val enabled: ViewMutableStateFlow<Boolean>
    val showOnlyWhenActive: ViewMutableStateFlow<Boolean>
    val position: ViewMutableStateFlow<DisplayPosition>
    val fillWidth: ViewMutableStateFlow<Boolean>
    val fontSize: ViewMutableStateFlow<Int>
    val boldFont: ViewMutableStateFlow<Boolean>
    val italicFont: ViewMutableStateFlow<Boolean>
    val textColor: ViewMutableStateFlow<String>
    val backgroundColor: ViewMutableStateFlow<String>
    val opacity: ViewMutableStateFlow<Double>
}

enum class DisplayPosition(val displayName: String) {
    TopLeft("Top Left"),
    TopCenter("Top Center"),
    TopRight("Top Right"),
    CenterLeft("Center Left"),
    Center("Center"),
    CenterRight("Center Right"),
    BottomLeft("Bottom Left"),
    BottomCenter("Bottom Center"),
    BottomRight("Bottom Right")
}