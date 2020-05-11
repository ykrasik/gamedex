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

package com.gitlab.ykrasik.gamedex.core.settings

import com.gitlab.ykrasik.gamedex.app.api.settings.DisplayPosition
import com.google.inject.BindingAnnotation

/**
 * User: ykrasik
 * Date: 09/06/2018
 * Time: 20:43
 */
class GameOverlayDisplaySettingsRepository(repo: SettingsRepository, name: String, default: () -> Data) {
    data class Data(
        val enabled: Boolean,
        val showOnlyWhenActive: Boolean,
        val position: DisplayPosition,
        val fillWidth: Boolean,
        val fontSize: Int,
        val boldFont: Boolean,
        val italicFont: Boolean,
        val textColor: String,
        val backgroundColor: String,
        val opacity: Double
    )

    private val storage = repo.storage(basePath = "display", name = name, default = default)

    val enabled = storage.biMap(Data::enabled) { copy(enabled = it) }
    val showOnlyWhenActive = storage.biMap(Data::showOnlyWhenActive) { copy(showOnlyWhenActive = it) }
    val position = storage.biMap(Data::position) { copy(position = it) }
    val fillWidth = storage.biMap(Data::fillWidth) { copy(fillWidth = it) }
    val fontSize = storage.biMap(Data::fontSize) { copy(fontSize = it) }
    val boldFont = storage.biMap(Data::boldFont) { copy(boldFont = it) }
    val italicFont = storage.biMap(Data::italicFont) { copy(italicFont = it) }
    val textColor = storage.biMap(Data::textColor) { copy(textColor = it) }
    val backgroundColor = storage.biMap(Data::backgroundColor) { copy(backgroundColor = it) }
    val opacity = storage.biMap(Data::opacity) { copy(opacity = it) }

    companion object {
        fun name(repo: SettingsRepository) = GameOverlayDisplaySettingsRepository(repo, name = "name") {
            Data(
                enabled = true,
                showOnlyWhenActive = true,
                position = DisplayPosition.TopCenter,
                fillWidth = true,
                fontSize = 13,
                boldFont = true,
                italicFont = false,
                textColor = "#ffffff",
                backgroundColor = "#62728C",
                opacity = 0.85
            )
        }

        fun metaTag(repo: SettingsRepository) = GameOverlayDisplaySettingsRepository(repo, name = "metatag") {
            Data(
                enabled = true,
                showOnlyWhenActive = true,
                position = DisplayPosition.BottomCenter,
                fillWidth = true,
                fontSize = 12,
                boldFont = false,
                italicFont = true,
                textColor = "#000000",
                backgroundColor = "#cce6ff",
                opacity = 0.85
            )
        }

        fun version(repo: SettingsRepository) = GameOverlayDisplaySettingsRepository(repo, name = "version") {
            Data(
                enabled = true,
                showOnlyWhenActive = true,
                position = DisplayPosition.BottomRight,
                fillWidth = false,
                fontSize = 16,
                boldFont = false,
                italicFont = true,
                textColor = "#000000",
                backgroundColor = "#D3D3D3",
                opacity = 0.85
            )
        }
    }
}

@BindingAnnotation
@Target(AnnotationTarget.FIELD, AnnotationTarget.FUNCTION, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class NameDisplaySettingsRepository

@BindingAnnotation
@Target(AnnotationTarget.FIELD, AnnotationTarget.FUNCTION, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class MetaTagDisplaySettingsRepository

@BindingAnnotation
@Target(AnnotationTarget.FIELD, AnnotationTarget.FUNCTION, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class VersionDisplaySettingsRepository