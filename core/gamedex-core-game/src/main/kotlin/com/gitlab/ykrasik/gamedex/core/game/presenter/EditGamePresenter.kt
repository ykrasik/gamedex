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

package com.gitlab.ykrasik.gamedex.core.game.presenter

import com.gitlab.ykrasik.gamedex.*
import com.gitlab.ykrasik.gamedex.app.api.game.EditGameView
import com.gitlab.ykrasik.gamedex.app.api.game.GameDataOverrideState
import com.gitlab.ykrasik.gamedex.app.api.game.OverrideSelectionType
import com.gitlab.ykrasik.gamedex.core.EventBus
import com.gitlab.ykrasik.gamedex.core.Presenter
import com.gitlab.ykrasik.gamedex.core.ViewSession
import com.gitlab.ykrasik.gamedex.core.game.GameService
import com.gitlab.ykrasik.gamedex.core.task.TaskService
import com.gitlab.ykrasik.gamedex.util.IsValid
import com.gitlab.ykrasik.gamedex.util.Try
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
class EditGamePresenter @Inject constructor(
    private val gameService: GameService,
    private val taskService: TaskService,
    private val eventBus: EventBus
) : Presenter<EditGameView> {
    override fun present(view: EditGameView) = object : ViewSession() {
        private val game by view.game

        private val allOverrides = with(view) {
            listOf(
                nameOverride,
                descriptionOverride,
                releaseDateOverride,
                criticScoreOverride,
                userScoreOverride,
                thumbnailUrlOverride,
                posterUrlOverride
            )
        }

        private lateinit var gameWithoutOverrides: Game

        init {
            allOverrides.forEach { it.initState() }

            view.canAccept *= IsValid.invalid("Nothing changed!")

            view.acceptActions.forEach { onAccept() }
            view.resetAllToDefaultActions.forEach { onResetAllToDefault() }
            view.cancelActions.forEach { onCancel() }
        }

        private fun <T> GameDataOverrideState<T>.initState() {
            selection.forEach { onSelectionChanged(it) }
            rawCustomValue.forEach { onRawCustomValueChanged(it) }
            customValueAcceptActions.forEach { onCustomValueAccepted() }
            resetToDefaultActions.forEach { onResetStateToDefault() }
        }

        private fun <T> GameDataOverrideState<T>.onSelectionChanged(selection: OverrideSelectionType?) {
            if (selection is OverrideSelectionType.Custom) {
                canSelectCustomOverride.assert()
            }

            setCanAccept()
        }

        private fun setCanAccept() {
            view.canAccept *= Try {
                val updatedGame = gameService.buildGame(calcUpdatedGame())
                check(updatedGame.gameData != game.gameData) { "Nothing changed!" }
            }
        }

        private fun GameDataOverrideState<*>.onRawCustomValueChanged(rawValue: String) {
            isCustomValueValid *= Try {
                if (rawValue.isEmpty()) error("Empty value!")
                try {
                    type.deserializeCustomValue<Any>(rawValue)
                } catch (_: Exception) {
                    error("Invalid $type!")
                }
            }
        }

        private fun <T> GameDataOverrideState<T>.onCustomValueAccepted() {
            isCustomValueValid.assert()
            customValue *= type.deserializeCustomValue(rawCustomValue.value)
            canSelectCustomOverride *= IsValid.valid
            selection *= OverrideSelectionType.Custom
            onSelectionChanged(selection.value)
        }

        private fun <T> GameDataOverrideState<T>.onResetStateToDefault() {
            val defaultValue = type.extractValue<T>(gameWithoutOverrides.gameData)
            selection *= if (defaultValue != null) findProviderWithValue(defaultValue) else null
            onSelectionChanged(selection.value)
        }

        private fun onResetAllToDefault() {
            allOverrides.forEach { it.onResetStateToDefault() }
        }

        override suspend fun onShown() {
            allOverrides.forEach { it.initStateOnShow() }

            gameWithoutOverrides = gameService.buildGame(game.rawGame.copy(userData = UserData.Null))

            view.canAccept *= IsValid.invalid("Nothing changed!")
        }

        @Suppress("UNCHECKED_CAST")
        private fun <T> GameDataOverrideState<T>.initStateOnShow() {
            providerValues *= game.providerData
                .mapNotNull { type.extractValue<T>(it.gameData)?.let { value -> it.providerId to value } }
                .toMap()

            when (val currentOverride = game.userData.overrides[type]) {
                is GameDataOverride.Custom -> {
                    selection *= OverrideSelectionType.Custom
                    canSelectCustomOverride *= IsValid.valid
                    rawCustomValue *= currentOverride.value.toString()
                    customValue *= type.deserializeCustomValue(rawCustomValue.value)
                    isCustomValueValid *= IsValid.valid
                }
                else -> {
                    selection *= if (currentOverride is GameDataOverride.Provider) {
                        OverrideSelectionType.Provider(currentOverride.provider)
                    } else {
                        val value = type.extractValue<T>(game.gameData)
                        if (value != null) {
                            findProviderWithValue(value)
                        } else {
                            null
                        }
                    }
                    canSelectCustomOverride *= IsValid.invalid("No custom $type!")
                    rawCustomValue *= ""
                    customValue *= null
                    isCustomValueValid *= IsValid.invalid("No custom $type!")
                }
            }
        }

        private fun <T> GameDataOverrideState<T>.findProviderWithValue(value: T): OverrideSelectionType? {
            // TODO: Would be more correct to use the order of providers from SettingsService
            return providerValues.value.entries
                .find { it.value == value }
                ?.let { OverrideSelectionType.Provider(it.key) }
        }

        @Suppress("UNCHECKED_CAST")
        private fun <T> GameDataType.deserializeCustomValue(value: String): T = when (this) {
            GameDataType.CriticScore, GameDataType.UserScore -> {
                val score = value.toDouble()
                check(score in 0.0..100.0) { "Must be in range [0, 100]!" }
                Score(score, numReviews = 0)
            }
            GameDataType.ReleaseDate -> {
                LocalDate.parse(value)
                value
            }
            GameDataType.Thumbnail, GameDataType.Poster -> {
                URL(value)
                value
            }
            else -> value
        } as T

        private suspend fun onAccept() {
            view.canAccept.assert()
            val newRawGame = calcUpdatedGame()
            taskService.execute(gameService.replace(game, newRawGame))
            hideView()
        }

        private fun onCancel() {
            hideView()
        }

        private fun hideView() = eventBus.requestHideView(view)

        private fun calcUpdatedGame(): RawGame {
            val overrides: Map<GameDataType, GameDataOverride> = allOverrides.mapNotNull { override ->
                val selection = override.selection.value
                val (value, gameDataOverride) = when (selection) {
                    is OverrideSelectionType.Custom -> {
                        val value = when (override.type) {
                            GameDataType.CriticScore, GameDataType.UserScore -> (override.customValue.value as Score).score
                            else -> override.customValue.value as Any
                        }
                        value to GameDataOverride.Custom(value)
                    }
                    is OverrideSelectionType.Provider ->
                        override.providerValues.value[selection.providerId] to GameDataOverride.Provider(selection.providerId)
                    else -> null to null
                }
                val defaultValue = override.type.extractValue<Any?>(gameWithoutOverrides.gameData)
                if (value != defaultValue) {
                    override.type to gameDataOverride!!
                } else {
                    null
                }
            }.toMap()

            val userData = game.userData.copy(overrides = overrides)
            return game.rawGame.copy(userData = userData)
        }
    }

    private companion object {
        @Suppress("UNCHECKED_CAST", "IMPLICIT_CAST_TO_ANY")
        fun <T> GameDataType.extractValue(gameData: GameData): T? = when (this) {
            GameDataType.Name -> gameData.name
            GameDataType.Description -> gameData.description
            GameDataType.ReleaseDate -> gameData.releaseDate
            GameDataType.CriticScore -> gameData.criticScore
            GameDataType.UserScore -> gameData.userScore
            GameDataType.Genres -> gameData.genres
            GameDataType.Thumbnail -> gameData.thumbnailUrl
            GameDataType.Poster -> gameData.posterUrl
            GameDataType.Screenshots -> gameData.screenshotUrls
        } as T?
    }
}