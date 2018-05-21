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

package com.gitlab.ykrasik.gamedex.app.api.game.edit

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.GameDataOverride
import com.gitlab.ykrasik.gamedex.GameDataType
import com.gitlab.ykrasik.gamedex.Score
import com.gitlab.ykrasik.gamedex.app.api.PresenterFactory
import com.gitlab.ykrasik.gamedex.app.api.image.Image
import com.gitlab.ykrasik.gamedex.provider.ProviderId
import kotlinx.coroutines.experimental.Deferred

/**
 * User: ykrasik
 * Date: 10/05/2018
 * Time: 08:08
 */
interface EditGamePresenter {
    fun onShown(game: Game, initialScreen: GameDataType)

    fun fetchImage(url: String): Deferred<Image>
    fun providerLogo(providerId: ProviderId): Image

    fun onProviderOverrideSelected(type: GameDataType, providerId: ProviderId, selected: Boolean)
    fun onCustomOverrideSelected(type: GameDataType, selected: Boolean)
    fun onClearOverrideSelected(type: GameDataType, selected: Boolean)
    fun onCustomOverrideValueChanged(type: GameDataType, rawValue: String)
    fun onCustomOverrideValueAccepted(type: GameDataType)
    fun onCustomOverrideValueRejected(type: GameDataType)

    fun onAccept()
    fun onClear()
    fun onCancel()
}

interface EditGameView {
    var game: Game
    var initialScreen: GameDataType

    var nameOverride: GameDataOverrideViewModel<String>
    var descriptionOverride: GameDataOverrideViewModel<String>
    var releaseDateOverride: GameDataOverrideViewModel<String>
    var criticScoreOverride: GameDataOverrideViewModel<Score>
    var userScoreOverride: GameDataOverrideViewModel<Score>
    var thumbnailUrlOverride: GameDataOverrideViewModel<String>
    var posterUrlOverride: GameDataOverrideViewModel<String>

    fun closeView()
}

data class GameDataOverrideViewModel<T>(
    val override: GameDataOverride? = null,
    val rawCustomValue: String = "",
    val customValue: T? = null,
    val customValueValidationError: String? = null
)

interface EditGamePresenterFactory : PresenterFactory<EditGameView, EditGamePresenter>