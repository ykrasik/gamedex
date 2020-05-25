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

package com.gitlab.ykrasik.gamedex.core.game.presenter

import com.gitlab.ykrasik.gamedex.*
import com.gitlab.ykrasik.gamedex.app.api.game.EditGameView
import com.gitlab.ykrasik.gamedex.app.api.game.GameDataOverrideState
import com.gitlab.ykrasik.gamedex.app.api.game.OverrideSelectionType
import com.gitlab.ykrasik.gamedex.core.EventBus
import com.gitlab.ykrasik.gamedex.core.game.GameService
import com.gitlab.ykrasik.gamedex.core.task.TaskService
import com.gitlab.ykrasik.gamedex.core.view.Presenter
import com.gitlab.ykrasik.gamedex.core.view.ViewSession
import com.gitlab.ykrasik.gamedex.util.IsValid
import com.gitlab.ykrasik.gamedex.util.existsOrNull
import com.gitlab.ykrasik.gamedex.util.file
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
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

        private val gameWithoutOverrides = MutableStateFlow(Game.Null)

        init {
            // TODO: At the time of writing this, there was a bug that caused combine(view.game.onlyValuesFromView(), isShowing) to behave incorrectly (miss the first value)
            combine(view.game.allValues(), isShowing) { game, isShowing -> game to isShowing }
                .forEach(debugName = "onGameChanged") { (game, isShowing) ->
                    if (isShowing) {
                        allOverrides.forEach {
                            it.initProviderAndCustomValues(game)
                        }
                        gameWithoutOverrides /= gameService.buildGame(game.rawGame.copy(userData = UserData.Null))
                    }
                }

            allOverrides.forEach { it.init() }

            view.absoluteMainExecutablePath *= view.game.onlyChangesFromView().combine(isShowing) { game, _ -> game.mainExecutableFile?.toString() ?: "" } withDebugName "absoluteMainExecutablePath"
            view::absoluteMainExecutablePathIsValid *= view.absoluteMainExecutablePath.allValues().map { validateMainExecutablePath(it) }
            view::canAccept *= view.absoluteMainExecutablePathIsValid

            view::browseMainExecutableActions.forEach { onBrowseMainExecutable() }

            view::acceptActions.forEach { onAccept() }
            view::resetAllToDefaultActions.forEach { onResetAllToDefault() }
            view::cancelActions.forEach { onCancel() }
        }

        private fun <T> GameDataOverrideState<T>.init() {
            selection.onlyChangesFromView().forEach(debugName = "$type.onSelectionChanged") { selection ->
                if (selection is OverrideSelectionType.Custom) {
                    canSelectCustomOverride.assert()
                }
            }

            isCustomValueValid *= rawCustomValue.allValues().map { rawValue ->
                IsValid {
                    if (rawValue.isEmpty()) error("Empty value!")
                    try {
                        type.deserializeCustomValue<Any>(rawValue)
                    } catch (_: Exception) {
                        error("Invalid $type!")
                    }
                }
            } withDebugName "$type.isCustomValueValid"

            customValueAcceptActions.forEach(debugName = "$type.onCustomValueAccepted") {
                isCustomValueValid.assert()
                customValue /= type.deserializeCustomValue<T>(rawCustomValue.v)
                canSelectCustomOverride /= IsValid.valid
                selection /= OverrideSelectionType.Custom
            }

            resetToDefaultActions.forEach(debugName = "$type.onResetToDefault") { onResetStateToDefault() }
        }

        private fun <T> GameDataOverrideState<T>.initProviderAndCustomValues(game: Game) {
            providerValues /= game.providerData
                .mapNotNull { type.extractValue<T>(it.gameData)?.let { value -> it.providerId to value } }
                .toMap()

            when (val currentOverride = game.userData.overrides[type]) {
                is GameDataOverride.Custom -> {
                    selection /= OverrideSelectionType.Custom
                    canSelectCustomOverride /= IsValid.valid
                    rawCustomValue /= currentOverride.value.toString()
                    customValue /= type.deserializeCustomValue<T>(rawCustomValue.v)
                    isCustomValueValid /= IsValid.valid
                }
                else -> {
                    selection /= if (currentOverride is GameDataOverride.Provider) {
                        OverrideSelectionType.Provider(currentOverride.provider)
                    } else {
                        val value = type.extractValue<T>(game.gameData)
                        if (value != null) {
                            findProviderWithValue(value)
                        } else {
                            null
                        }
                    }
                    canSelectCustomOverride /= IsValid.invalid("No custom $type!")
                    rawCustomValue /= ""
                    customValue /= null
                    isCustomValueValid /= IsValid.invalid("No custom $type!")
                }
            }
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

        private fun <T> GameDataOverrideState<T>.onResetStateToDefault() {
            val defaultValue = type.extractValue<T>(gameWithoutOverrides.value.gameData)
            selection /= if (defaultValue != null) findProviderWithValue(defaultValue) else null
        }

        private fun <T> GameDataOverrideState<T>.findProviderWithValue(value: T): OverrideSelectionType? {
            // TODO: Would be more correct to use the order of providers from SettingsService
            return providerValues.value.entries
                .find { it.value == value }
                ?.let { OverrideSelectionType.Provider(it.key) }
        }

        private fun onResetAllToDefault() {
            allOverrides.forEach { it.onResetStateToDefault() }
            view.absoluteMainExecutablePath /= game.mainExecutableFile?.toString() ?: ""
        }

        private suspend fun onAccept() {
            view.canAccept.assert()
            val newRawGame = calcUpdatedGame()
            if (newRawGame.userData != game.userData) {
                taskService.execute(gameService.replace(game, newRawGame))
            }
            hideView()
        }

        private fun calcUpdatedGame(): RawGame {
            val overrides: Map<GameDataType, GameDataOverride> = allOverrides.mapNotNull { override ->
                val selection = override.selection.v
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
                val defaultValue = override.type.extractValue<Any?>(gameWithoutOverrides.value.gameData)
                if (value != defaultValue) {
                    override.type to gameDataOverride!!
                } else {
                    null
                }
            }.toMap()

            val userData = game.userData.copy(
                overrides = overrides,
                mainExecutablePath = view.absoluteMainExecutablePath.v.takeIf { it.isNotBlank() }
            )
            return game.rawGame.copy(userData = userData)
        }

        private fun onCancel() {
            hideView()
        }

        private fun hideView() = eventBus.requestHideView(view)

        private fun onBrowseMainExecutable() {
            val selectedDirectory = view.browse(initialDirectory = game.path.existsOrNull())
            if (selectedDirectory != null) {
                view.absoluteMainExecutablePath /= selectedDirectory.toString()
            }
        }

        private fun validateMainExecutablePath(absoluteMainExecutablePath: String) = IsValid {
            if (absoluteMainExecutablePath.isEmpty() || absoluteMainExecutablePath == game.mainExecutableFile?.toString()) return@IsValid
            val file = absoluteMainExecutablePath.file
            check(file.exists()) { "File doesn't exist!" }
            check(!file.isDirectory) { "Main Executable must not be a directory!" }
            check(file.startsWith(game.path)) { "Main Executable must be under the game's path!" }
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