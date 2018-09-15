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

package com.gitlab.ykrasik.gamedex.core.settings.presenter

import com.gitlab.ykrasik.gamedex.app.api.settings.*
import com.gitlab.ykrasik.gamedex.core.Presentation
import com.gitlab.ykrasik.gamedex.core.Presenter
import com.gitlab.ykrasik.gamedex.core.settings.AbstractGameOverlayDisplaySettingsRepository
import com.gitlab.ykrasik.gamedex.core.settings.SettingsService
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 09/06/2018
 * Time: 18:59
 */
abstract class ChangeGameOverlayDisplaySettingsPresenter<V> : Presenter<V> {
    override fun present(view: V) = object : Presentation() {
        init {
            with(extractOverlay(view)) {
                repo.bind({ enabledChannel }, ::enabled, enabledChanges) { copy(enabled = it) }
                repo.bind({ showOnlyWhenActiveChannel }, ::showOnlyWhenActive, showOnlyWhenActiveChanges) { copy(showOnlyWhenActive = it) }
                repo.bind({ positionChannel }, ::position, positionChanges) { copy(position = it) }
                repo.bind({ fillWidthChannel }, ::fillWidth, fillWidthChanges) { copy(fillWidth = it) }
                repo.bind({ fontSizeChannel }, ::fontSize, fontSizeChanges) { copy(fontSize = it) }
                repo.bind({ boldFontChannel }, ::boldFont, boldFontChanges) { copy(boldFont = it) }
                repo.bind({ italicFontChannel }, ::italicFont, italicFontChanges) { copy(italicFont = it) }
                repo.bind({ textColorChannel }, ::textColor, textColorChanges) { copy(textColor = it) }
                repo.bind({ backgroundColorChannel }, ::backgroundColor, backgroundColorChanges) { copy(backgroundColor = it) }
                repo.bind({ opacityChannel }, ::opacity, opacityChanges) { copy(opacity = it) }
            }
        }
    }

    protected abstract val repo: AbstractGameOverlayDisplaySettingsRepository
    protected abstract fun extractOverlay(view: V): MutableOverlayDisplaySettings
}

@Singleton
class ChangeGameNameOverlayDisplaySettingsPresenter @Inject constructor(private val settingsService: SettingsService) :
    ChangeGameOverlayDisplaySettingsPresenter<ViewCanChangeNameOverlayDisplaySettings>() {
    override val repo get() = settingsService.nameDisplay
    override fun extractOverlay(view: ViewCanChangeNameOverlayDisplaySettings) = view.mutableNameOverlayDisplaySettings
}

@Singleton
class ChangeGameMetaTagOverlayDisplaySettingsPresenter @Inject constructor(private val settingsService: SettingsService) :
    ChangeGameOverlayDisplaySettingsPresenter<ViewCanChangeMetaTagOverlayDisplaySettings>() {
    override val repo get() = settingsService.metaTagDisplay
    override fun extractOverlay(view: ViewCanChangeMetaTagOverlayDisplaySettings) = view.mutableMetaTagOverlayDisplaySettings
}

@Singleton
class ChangeGameVersionOverlayDisplaySettingsPresenter @Inject constructor(private val settingsService: SettingsService) :
    ChangeGameOverlayDisplaySettingsPresenter<ViewCanChangeVersionOverlayDisplaySettings>() {
    override val repo get() = settingsService.versionDisplay
    override fun extractOverlay(view: ViewCanChangeVersionOverlayDisplaySettings) = view.mutableVersionOverlayDisplaySettings
}

abstract class GameOverlayDisplaySettingsPresenter<V> : Presenter<V> {
    override fun present(view: V) = object : Presentation() {
        init {
            with(extractOverlay(view)) {
                repo.enabledChannel.reportChangesTo(::enabled)
                repo.showOnlyWhenActiveChannel.reportChangesTo(::showOnlyWhenActive)
                repo.positionChannel.reportChangesTo(::position)
                repo.fillWidthChannel.reportChangesTo(::fillWidth)
                repo.fontSizeChannel.reportChangesTo(::fontSize)
                repo.boldFontChannel.reportChangesTo(::boldFont)
                repo.italicFontChannel.reportChangesTo(::italicFont)
                repo.textColorChannel.reportChangesTo(::textColor)
                repo.backgroundColorChannel.reportChangesTo(::backgroundColor)
                repo.opacityChannel.reportChangesTo(::opacity)
            }
        }
    }

    protected abstract val repo: AbstractGameOverlayDisplaySettingsRepository
    protected abstract fun extractOverlay(view: V): com.gitlab.ykrasik.gamedex.app.api.settings.OverlayDisplaySettings
}

@Singleton
class GameNameOverlayDisplaySettingsPresenter @Inject constructor(private val settingsService: SettingsService) :
    GameOverlayDisplaySettingsPresenter<ViewWithNameOverlayDisplaySettings>() {
    override val repo get() = settingsService.nameDisplay
    override fun extractOverlay(view: ViewWithNameOverlayDisplaySettings) = view.nameOverlayDisplaySettings
}

@Singleton
class GameMetaTagOverlayDisplaySettingsPresenter @Inject constructor(private val settingsService: SettingsService) :
    GameOverlayDisplaySettingsPresenter<ViewWithMetaTagOverlayDisplaySettings>() {
    override val repo get() = settingsService.metaTagDisplay
    override fun extractOverlay(view: ViewWithMetaTagOverlayDisplaySettings) = view.metaTagOverlayDisplaySettings
}

@Singleton
class GameVersionOverlayDisplaySettingsPresenter @Inject constructor(private val settingsService: SettingsService) :
    GameOverlayDisplaySettingsPresenter<ViewWithVersionOverlayDisplaySettings>() {
    override val repo get() = settingsService.versionDisplay
    override fun extractOverlay(view: ViewWithVersionOverlayDisplaySettings) = view.versionOverlayDisplaySettings
}