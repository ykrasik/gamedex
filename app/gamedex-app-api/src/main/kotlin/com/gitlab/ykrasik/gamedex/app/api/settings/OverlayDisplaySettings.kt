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

package com.gitlab.ykrasik.gamedex.app.api.settings

import com.gitlab.ykrasik.gamedex.app.api.util.StatefulChannel
import com.gitlab.ykrasik.gamedex.app.api.util.ViewMutableStatefulChannel

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
    val enabled: StatefulChannel<Boolean>
    val showOnlyWhenActive: StatefulChannel<Boolean>
    val position: StatefulChannel<DisplayPosition>
    val fillWidth: StatefulChannel<Boolean>
    val fontSize: StatefulChannel<Int>
    val boldFont: StatefulChannel<Boolean>
    val italicFont: StatefulChannel<Boolean>
    val textColor: StatefulChannel<String>
    val backgroundColor: StatefulChannel<String>
    val opacity: StatefulChannel<Double>
}

interface MutableOverlayDisplaySettings {
    val enabled: ViewMutableStatefulChannel<Boolean>
    val showOnlyWhenActive: ViewMutableStatefulChannel<Boolean>
    val position: ViewMutableStatefulChannel<DisplayPosition>
    val fillWidth: ViewMutableStatefulChannel<Boolean>
    val fontSize: ViewMutableStatefulChannel<Int>
    val boldFont: ViewMutableStatefulChannel<Boolean>
    val italicFont: ViewMutableStatefulChannel<Boolean>
    val textColor: ViewMutableStatefulChannel<String>
    val backgroundColor: ViewMutableStatefulChannel<String>
    val opacity: ViewMutableStatefulChannel<Double>
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