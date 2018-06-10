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

package com.gitlab.ykrasik.gamedex.core.settings

import com.gitlab.ykrasik.gamedex.app.api.settings.*
import com.gitlab.ykrasik.gamedex.core.Presenter
import com.gitlab.ykrasik.gamedex.core.PresenterFactory
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 09/06/2018
 * Time: 18:59
 */
abstract class ChangeGameOverlayDisplaySettingsPresenterFactory<V> constructor(
    private val settingsService: SettingsService
) : PresenterFactory<V> {
    override fun present(view: V) = object : Presenter() {
        init {
            val settings = extractSettings(settingsService.gameDisplay)
            with(extractOverlay(view)) {
                settings.enabledChannel.bind(::enabled, enabledChanges)
                settings.showOnlyWhenActiveChannel.bind(::showOnlyWhenActive, showOnlyWhenActiveChanges)
                settings.positionChannel.bind(::position, positionChanges)
                settings.fillWidthChannel.bind(::fillWidth, fillWidthChanges)
                settings.fontSizeChannel.bind(::fontSize, fontSizeChanges)
                settings.boldFontChannel.bind(::boldFont, boldFontChanges)
                settings.italicFontChannel.bind(::italicFont, italicFontChanges)
                settings.textColorChannel.bind(::textColor, textColorChanges)
                settings.backgroundColorChannel.bind(::backgroundColor, backgroundColorChanges)
                settings.opacityChannel.bind(::opacity, opacityChanges)
            }
        }
    }

    protected abstract fun extractSettings(repo: GameDisplaySettingsRepository): GameDisplaySettingsRepository.OverlaySettingsAccessor
    protected abstract fun extractOverlay(view: V): MutableOverlayDisplaySettings
}

@Singleton
class ChangeGameNameOverlayDisplaySettingsPresenterFactory @Inject constructor(settingsService: SettingsService) :
    ChangeGameOverlayDisplaySettingsPresenterFactory<ViewCanChangeNameOverlayDisplaySettings>(settingsService) {
    override fun extractSettings(repo: GameDisplaySettingsRepository) = repo.nameOverlay
    override fun extractOverlay(view: ViewCanChangeNameOverlayDisplaySettings) = view.mutableNameOverlayDisplaySettings
}

@Singleton
class ChangeGameMetaTagOverlayDisplaySettingsPresenterFactory @Inject constructor(settingsService: SettingsService) :
    ChangeGameOverlayDisplaySettingsPresenterFactory<ViewCanChangeMetaTagOverlayDisplaySettings>(settingsService) {
    override fun extractSettings(repo: GameDisplaySettingsRepository) = repo.metaTagOverlay
    override fun extractOverlay(view: ViewCanChangeMetaTagOverlayDisplaySettings) = view.mutableMetaTagOverlayDisplaySettings
}

@Singleton
class ChangeGameVersionOverlayDisplaySettingsPresenterFactory @Inject constructor(settingsService: SettingsService) :
    ChangeGameOverlayDisplaySettingsPresenterFactory<ViewCanChangeVersionOverlayDisplaySettings>(settingsService) {
    override fun extractSettings(repo: GameDisplaySettingsRepository) = repo.versionOverlay
    override fun extractOverlay(view: ViewCanChangeVersionOverlayDisplaySettings) = view.mutableVersionOverlayDisplaySettings
}

abstract class GameOverlayDisplaySettingsPresenterFactory<V> constructor(
    private val settingsService: SettingsService
) : PresenterFactory<V> {
    override fun present(view: V) = object : Presenter() {
        init {
            val settings = extractSettings(settingsService.gameDisplay)
            with(extractOverlay(view)) {
                settings.enabledChannel.reportChangesTo(::enabled)
                settings.showOnlyWhenActiveChannel.reportChangesTo(::showOnlyWhenActive)
                settings.positionChannel.reportChangesTo(::position)
                settings.fillWidthChannel.reportChangesTo(::fillWidth)
                settings.fontSizeChannel.reportChangesTo(::fontSize)
                settings.boldFontChannel.reportChangesTo(::boldFont)
                settings.italicFontChannel.reportChangesTo(::italicFont)
                settings.textColorChannel.reportChangesTo(::textColor)
                settings.backgroundColorChannel.reportChangesTo(::backgroundColor)
                settings.opacityChannel.reportChangesTo(::opacity)
            }
        }
    }

    protected abstract fun extractSettings(repo: GameDisplaySettingsRepository): GameDisplaySettingsRepository.OverlaySettingsAccessor
    protected abstract fun extractOverlay(view: V): OverlayDisplaySettings
}

@Singleton
class GameNameOverlayDisplaySettingsPresenterFactory @Inject constructor(settingsService: SettingsService) :
    GameOverlayDisplaySettingsPresenterFactory<ViewWithNameOverlayDisplaySettings>(settingsService) {
    override fun extractSettings(repo: GameDisplaySettingsRepository) = repo.nameOverlay
    override fun extractOverlay(view: ViewWithNameOverlayDisplaySettings) = view.nameOverlayDisplaySettings
}

@Singleton
class GameMetaTagOverlayDisplaySettingsPresenterFactory @Inject constructor(settingsService: SettingsService) :
    GameOverlayDisplaySettingsPresenterFactory<ViewWithMetaTagOverlayDisplaySettings>(settingsService) {
    override fun extractSettings(repo: GameDisplaySettingsRepository) = repo.metaTagOverlay
    override fun extractOverlay(view: ViewWithMetaTagOverlayDisplaySettings) = view.metaTagOverlayDisplaySettings
}

@Singleton
class GameVersionOverlayDisplaySettingsPresenterFactory @Inject constructor(settingsService: SettingsService) :
    GameOverlayDisplaySettingsPresenterFactory<ViewWithVersionOverlayDisplaySettings>(settingsService) {
    override fun extractSettings(repo: GameDisplaySettingsRepository) = repo.versionOverlay
    override fun extractOverlay(view: ViewWithVersionOverlayDisplaySettings) = view.versionOverlayDisplaySettings
}