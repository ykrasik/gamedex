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

package com.gitlab.ykrasik.gamedex.app.api.game

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.GameDataOverride
import com.gitlab.ykrasik.gamedex.GameDataType
import com.gitlab.ykrasik.gamedex.Score
import com.gitlab.ykrasik.gamedex.app.api.image.Image
import com.gitlab.ykrasik.gamedex.app.api.util.IsValid
import com.gitlab.ykrasik.gamedex.provider.ProviderId
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.channels.ReceiveChannel

/**
 * User: ykrasik
 * Date: 10/05/2018
 * Time: 08:08
 */
interface EditGameView {
    var initialScreen: GameDataType
    var game: Game

    val fetchThumbnailRequests: ReceiveChannel<FetchThumbnailRequest>

    var nameOverride: GameDataOverrideViewModel<String>
    var descriptionOverride: GameDataOverrideViewModel<String>
    var releaseDateOverride: GameDataOverrideViewModel<String>
    var criticScoreOverride: GameDataOverrideViewModel<Score>
    var userScoreOverride: GameDataOverrideViewModel<Score>
    var thumbnailUrlOverride: GameDataOverrideViewModel<String>
    var posterUrlOverride: GameDataOverrideViewModel<String>

    val providerOverrideSelectionChanges: ReceiveChannel<Triple<GameDataType, ProviderId, Boolean>>
    val customOverrideSelectionChanges: ReceiveChannel<Pair<GameDataType, Boolean>>
    val clearOverrideSelectionChanges: ReceiveChannel<Pair<GameDataType, Boolean>>

    val customOverrideValueChanges: ReceiveChannel<Pair<GameDataType, String>>
    val customOverrideValueAcceptActions: ReceiveChannel<GameDataType>
    val customOverrideValueRejectActions: ReceiveChannel<GameDataType>

    val acceptActions: ReceiveChannel<Unit>
    val clearActions: ReceiveChannel<Unit>
    val cancelActions: ReceiveChannel<Unit>
}

data class GameDataOverrideViewModel<T>(
    val override: GameDataOverride? = null,
    val rawCustomValue: String = "",
    val customValue: T? = null,
    val isCustomValueValid: IsValid = IsValid.valid
)

data class FetchThumbnailRequest(
    val url: String,
    val response: CompletableDeferred<Deferred<Image>>
)