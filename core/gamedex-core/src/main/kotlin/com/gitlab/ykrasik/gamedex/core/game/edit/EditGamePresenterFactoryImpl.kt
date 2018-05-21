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

package com.gitlab.ykrasik.gamedex.core.game.edit

import com.gitlab.ykrasik.gamedex.*
import com.gitlab.ykrasik.gamedex.app.api.game.edit.EditGamePresenter
import com.gitlab.ykrasik.gamedex.app.api.game.edit.EditGamePresenterFactory
import com.gitlab.ykrasik.gamedex.app.api.game.edit.EditGameView
import com.gitlab.ykrasik.gamedex.app.api.game.edit.GameDataOverrideViewModel
import com.gitlab.ykrasik.gamedex.app.api.image.Image
import com.gitlab.ykrasik.gamedex.app.api.task.TaskRunner
import com.gitlab.ykrasik.gamedex.core.api.game.GameService
import com.gitlab.ykrasik.gamedex.core.api.image.ImageRepository
import com.gitlab.ykrasik.gamedex.core.api.provider.GameProviderService
import com.gitlab.ykrasik.gamedex.core.launchOnUi
import com.gitlab.ykrasik.gamedex.provider.ProviderId
import kotlinx.coroutines.experimental.Deferred
import org.joda.time.LocalDate
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 10/05/2018
 * Time: 08:10
 */
@Singleton
class EditGamePresenterFactoryImpl @Inject constructor(
    private val imageRepository: ImageRepository,
    private val gameProviderService: GameProviderService,
    private val gameService: GameService,
    private val taskRunner: TaskRunner
) : EditGamePresenterFactory {
    override fun present(view: EditGameView): EditGamePresenter = object : EditGamePresenter {
        override fun onShown(game: Game, initialScreen: GameDataType) {
            view.game = game
            view.initialScreen = initialScreen // TODO: is this needed here?

            view.nameOverride = initViewModel(GameDataType.name_)
            view.descriptionOverride = initViewModel(GameDataType.description)
            view.releaseDateOverride = initViewModel(GameDataType.releaseDate)
            view.criticScoreOverride = initViewModel(GameDataType.criticScore)
            view.userScoreOverride = initViewModel(GameDataType.userScore)
            view.thumbnailUrlOverride = initViewModel(GameDataType.thumbnail)
            view.posterUrlOverride = initViewModel(GameDataType.poster)
        }

        override fun fetchImage(url: String): Deferred<Image> = imageRepository.fetchImage(url, view.game.id, persistIfAbsent = false)

        override fun providerLogo(providerId: ProviderId): Image = gameProviderService.logos[providerId]!!

        override fun onProviderOverrideSelected(type: GameDataType, providerId: ProviderId, selected: Boolean) {
            if (selected) {
                modifyOverride(type) { it.copy(override = GameDataOverride.Provider(providerId)) }
            }
        }

        override fun onCustomOverrideSelected(type: GameDataType, selected: Boolean) {
            if (selected) {
                modifyOverride(type) { current ->
                    check(current.customValueValidationError == null) { "Cannot set a custom '$type' with a validation error!" }
                    current.copy(override = GameDataOverride.Custom(current.customValue!!))
                }
            }
        }

        override fun onClearOverrideSelected(type: GameDataType, selected: Boolean) {
            if (selected) {
                modifyOverride(type) { it.copy(override = null) }
            }
        }

        override fun onCustomOverrideValueChanged(type: GameDataType, rawValue: String) {
            val error = if (rawValue.isEmpty()) {
                ""
            } else {
                try {
                    rawValue.deserializeCustom(type)
                    null
                } catch (e: Exception) {
                    "Invalid ${type.displayName}"
                }
            }
            modifyOverride(type) { it.copy(rawCustomValue = rawValue, customValueValidationError = error) }
        }

        override fun onCustomOverrideValueAccepted(type: GameDataType) {
            modifyOverride(type) { current ->
                check(current.customValueValidationError == null) { "Cannot accept a custom '$type' with a validation error!" }
                val customValue = current.rawCustomValue.deserializeCustom(type)
                current.copy(customValue = customValue, override = GameDataOverride.Custom(customValue))
            }
        }

        override fun onCustomOverrideValueRejected(type: GameDataType) {
        }

        private fun String.deserializeCustom(type: GameDataType): Any = when (type) {
            GameDataType.criticScore, GameDataType.userScore -> Score(this.toDouble(), numReviews = 0)
            GameDataType.releaseDate -> {
                LocalDate.parse(this)
                this
            }
            GameDataType.thumbnail, GameDataType.poster -> {
                URL(this)
                this
            }
            else -> this
        }

        private fun Any.serializeCustom(type: GameDataType): String = when (type) {
            GameDataType.criticScore, GameDataType.userScore -> (this as Score).score.toString()
            else -> this as String
        }

        @Suppress("UNCHECKED_CAST")
        private fun <T> initViewModel(type: GameDataType): GameDataOverrideViewModel<T> {
            val currentOverride = (view.game.rawGame.userData?.overrides ?: emptyMap())[type]
            return if (currentOverride == null) {
                GameDataOverrideViewModel(customValueValidationError = "")
            } else {
                val (rawValue, value) = when (currentOverride) {
                    is GameDataOverride.Custom -> currentOverride.value.serializeCustom(type) to currentOverride.value
                    else -> "" to null
                }
                GameDataOverrideViewModel(
                    override = currentOverride,
                    rawCustomValue = rawValue,
                    customValue = value
                ) as GameDataOverrideViewModel<T>
            }
        }

        @Suppress("UNCHECKED_CAST")
        private fun setOverride(type: GameDataType, overrideViewModel: GameDataOverrideViewModel<*>) = when (type) {
            GameDataType.name_ -> view.nameOverride = overrideViewModel as GameDataOverrideViewModel<String>
            GameDataType.description -> view.descriptionOverride = overrideViewModel as GameDataOverrideViewModel<String>
            GameDataType.releaseDate -> view.releaseDateOverride = overrideViewModel as GameDataOverrideViewModel<String>
            GameDataType.criticScore -> view.criticScoreOverride = overrideViewModel as GameDataOverrideViewModel<Score>
            GameDataType.userScore -> view.userScoreOverride = overrideViewModel as GameDataOverrideViewModel<Score>
            GameDataType.thumbnail -> view.thumbnailUrlOverride = overrideViewModel as GameDataOverrideViewModel<String>
            GameDataType.poster -> view.posterUrlOverride = overrideViewModel as GameDataOverrideViewModel<String>
            else -> error("Unsupported GameDataType for edit: $type")
        }

        @Suppress("UNCHECKED_CAST")
        private fun currentOverride(type: GameDataType): GameDataOverrideViewModel<Any> = when (type) {
            GameDataType.name_ -> view.nameOverride
            GameDataType.description -> view.descriptionOverride
            GameDataType.releaseDate -> view.releaseDateOverride
            GameDataType.criticScore -> view.criticScoreOverride
            GameDataType.userScore -> view.userScoreOverride
            GameDataType.thumbnail -> view.thumbnailUrlOverride
            GameDataType.poster -> view.posterUrlOverride
            else -> error("Unsupported GameDataType for edit: $type")
        } as GameDataOverrideViewModel<Any>

        private inline fun modifyOverride(type: GameDataType, f: (GameDataOverrideViewModel<Any>) -> GameDataOverrideViewModel<Any>) {
            setOverride(type, f(currentOverride(type)))
        }

        override fun onAccept() {
            val overrides = listOf(
                GameDataType.name_ to view.nameOverride,
                GameDataType.description to view.descriptionOverride,
                GameDataType.releaseDate to view.releaseDateOverride,
                GameDataType.criticScore to view.criticScoreOverride,
                GameDataType.userScore to view.userScoreOverride,
                GameDataType.thumbnail to view.thumbnailUrlOverride,
                GameDataType.poster to view.posterUrlOverride
            ).mapNotNull { (type, override) ->
                override.override?.let { type to it }
            }.toMap()

            writeOverrides(overrides)
            view.closeView()
        }

        override fun onClear() {
            writeOverrides(emptyMap())
            view.closeView()
        }

        override fun onCancel() {
            view.closeView()
        }

        private fun writeOverrides(overrides: Map<GameDataType, GameDataOverride>) = launchOnUi {
            val newRawGame = view.game.rawGame.withDataOverrides(overrides)
            if (newRawGame.userData != view.game.rawGame.userData) {
                taskRunner.runTask(gameService.replace(view.game, newRawGame))
            }
        }

        private fun RawGame.withDataOverrides(overrides: Map<GameDataType, GameDataOverride>): RawGame {
            // If new overrides are empty and userData is null, or userData has empty overrides -> nothing to do
            // If new overrides are not empty and userData is not null, but has the same overrides -> nothing to do
            if (overrides == userData?.overrides ?: emptyMap<GameDataType, GameDataOverride>()) return this

            val userData = this.userData ?: UserData()
            return copy(userData = userData.copy(overrides = overrides))
        }
    }
}