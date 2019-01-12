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

package com.gitlab.ykrasik.gamedex.app.api.settings

import com.gitlab.ykrasik.gamedex.app.api.State
import com.gitlab.ykrasik.gamedex.app.api.UserMutableState

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
    val enabled: State<Boolean>
    val showOnlyWhenActive: State<Boolean>
    val position: State<DisplayPosition>
    val fillWidth: State<Boolean>
    val fontSize: State<Int>
    val boldFont: State<Boolean>
    val italicFont: State<Boolean>
    val textColor: State<String>
    val backgroundColor: State<String>
    val opacity: State<Double>
}

interface MutableOverlayDisplaySettings {
    val enabled: UserMutableState<Boolean>
    val showOnlyWhenActive: UserMutableState<Boolean>
    val position: UserMutableState<DisplayPosition>
    val fillWidth: UserMutableState<Boolean>
    val fontSize: UserMutableState<Int>
    val boldFont: UserMutableState<Boolean>
    val italicFont: UserMutableState<Boolean>
    val textColor: UserMutableState<String>
    val backgroundColor: UserMutableState<String>
    val opacity: UserMutableState<Double>
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
    BottomRight("Bottom Right");

    override fun toString() = displayName
}