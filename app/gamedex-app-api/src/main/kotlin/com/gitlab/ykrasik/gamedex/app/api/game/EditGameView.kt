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

package com.gitlab.ykrasik.gamedex.app.api.game

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.GameDataType
import com.gitlab.ykrasik.gamedex.Score
import com.gitlab.ykrasik.gamedex.app.api.ConfirmationView
import com.gitlab.ykrasik.gamedex.app.api.util.ViewMutableStateFlow
import com.gitlab.ykrasik.gamedex.provider.ProviderId
import com.gitlab.ykrasik.gamedex.util.IsValid
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File

/**
 * User: ykrasik
 * Date: 10/05/2018
 * Time: 08:08
 */
interface EditGameView : ConfirmationView {
    val game: ViewMutableStateFlow<Game>

    val nameOverride: GameDataOverrideState<String>
    val descriptionOverride: GameDataOverrideState<String>
    val releaseDateOverride: GameDataOverrideState<String>
    val criticScoreOverride: GameDataOverrideState<Score>
    val userScoreOverride: GameDataOverrideState<Score>
    val thumbnailUrlOverride: GameDataOverrideState<String>
    val posterUrlOverride: GameDataOverrideState<String>

    val absoluteMainExecutablePath: ViewMutableStateFlow<String>
    val absoluteMainExecutablePathIsValid: MutableStateFlow<IsValid>

    val browseMainExecutableActions: Flow<Unit>
    fun browse(initialDirectory: File?): File?

    val resetAllToDefaultActions: Flow<Unit>
}

interface GameDataOverrideState<T> {
    val type: GameDataType
    val customValue: MutableStateFlow<T?>
    val providerValues: MutableStateFlow<Map<ProviderId, T>>
    val selection: ViewMutableStateFlow<OverrideSelectionType?>

    val canSelectCustomOverride: MutableStateFlow<IsValid>
    val rawCustomValue: ViewMutableStateFlow<String>
    val isCustomValueValid: MutableStateFlow<IsValid>
    val customValueAcceptActions: Flow<Unit>
    val customValueRejectActions: Flow<Unit>

    val resetToDefaultActions: Flow<Unit>
}

sealed class OverrideSelectionType {
    data class Provider(val providerId: ProviderId) : OverrideSelectionType()
    object Custom : OverrideSelectionType()
}
