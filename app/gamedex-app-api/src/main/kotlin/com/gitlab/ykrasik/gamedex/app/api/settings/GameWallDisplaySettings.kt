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

import com.gitlab.ykrasik.gamedex.app.api.util.ViewMutableStateFlow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * User: ykrasik
 * Date: 10/06/2018
 * Time: 12:23
 */
interface ViewWithGameWallDisplaySettings {
    val gameWallDisplaySettings: GameWallDisplaySettings
}

interface ViewCanChangeGameWallDisplaySettings {
    val mutableGameWallDisplaySettings: MutableGameWallDisplaySettings
}

interface GameWallDisplaySettings {
    val imageDisplayType: MutableStateFlow<ImageDisplayType>
    val showBorder: MutableStateFlow<Boolean>
    val width: MutableStateFlow<Double>
    val height: MutableStateFlow<Double>
    val horizontalSpacing: MutableStateFlow<Double>
    val verticalSpacing: MutableStateFlow<Double>
}

interface MutableGameWallDisplaySettings {
    val imageDisplayType: ViewMutableStateFlow<ImageDisplayType>
    val showBorder: ViewMutableStateFlow<Boolean>
    val width: ViewMutableStateFlow<Double>
    val height: ViewMutableStateFlow<Double>
    val horizontalSpacing: ViewMutableStateFlow<Double>
    val verticalSpacing: ViewMutableStateFlow<Double>
}

enum class ImageDisplayType(val displayName: String) {
    Fixed("Fixed"),
    Fit("Fit"),
    Stretch("Stretch")
}