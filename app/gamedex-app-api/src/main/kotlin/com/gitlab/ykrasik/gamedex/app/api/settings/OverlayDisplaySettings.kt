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

package com.gitlab.ykrasik.gamedex.app.api.settings

import kotlinx.coroutines.experimental.channels.ReceiveChannel

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
    var enabled: Boolean
    var showOnlyWhenActive: Boolean
    var position: DisplayPosition
    var fillWidth: Boolean
    var fontSize: Int
    var boldFont: Boolean
    var italicFont: Boolean
    var textColor: String
    var backgroundColor: String
    var opacity: Double
}

interface MutableOverlayDisplaySettings : OverlayDisplaySettings {
    val enabledChanges: ReceiveChannel<Boolean>
    val showOnlyWhenActiveChanges: ReceiveChannel<Boolean>
    val positionChanges: ReceiveChannel<DisplayPosition>
    val fillWidthChanges: ReceiveChannel<Boolean>
    val fontSizeChanges: ReceiveChannel<Int>
    val boldFontChanges: ReceiveChannel<Boolean>
    val italicFontChanges: ReceiveChannel<Boolean>
    val textColorChanges: ReceiveChannel<String>
    val backgroundColorChanges: ReceiveChannel<String>
    val opacityChanges: ReceiveChannel<Double>
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