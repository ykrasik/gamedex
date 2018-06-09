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
 * Time: 12:23
 */
interface ViewWithGameCellDisplaySettings {
    val cellDisplaySettings: CellDisplaySettings
}

interface ViewCanChangeGameCellDisplaySettings {
    val mutableCellDisplaySettings: MutableCellDisplaySettings
}

interface CellDisplaySettings {
    var imageDisplayType: ImageDisplayType
    var showBorder: Boolean
    var width: Double
    var height: Double
    var horizontalSpacing: Double
    var verticalSpacing: Double
}

interface MutableCellDisplaySettings : CellDisplaySettings {
    val imageDisplayTypeChanges: ReceiveChannel<ImageDisplayType>
    val showBorderChanges: ReceiveChannel<Boolean>
    val widthChanges: ReceiveChannel<Double>
    val heightChanges: ReceiveChannel<Double>
    val horizontalSpacingChanges: ReceiveChannel<Double>
    val verticalSpacingChanges: ReceiveChannel<Double>
}

enum class ImageDisplayType(val displayName: String) {
    FixedSize("Fixed Size"),
    Fit("Fit"),
    Stretch("Stretch");

    override fun toString() = displayName
}