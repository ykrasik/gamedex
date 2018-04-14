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

package com.gitlab.ykrasik.gamedex.ui.view.settings

import com.gitlab.ykrasik.gamedex.core.game.GameSettings
import com.gitlab.ykrasik.gamedex.javafx.*
import com.gitlab.ykrasik.gamedex.javafx.control.adjustableTextField
import com.gitlab.ykrasik.gamedex.javafx.game.GameWallSettings
import javafx.geometry.Pos
import tornadofx.*

/**
 * User: ykrasik
 * Date: 05/06/2017
 * Time: 15:21
 */
class GameSettingsView : View("Game Settings", Theme.Icon.games()) {
    private val gameSettings: GameSettings by di()
    private val gameWallSettings: GameWallSettings by di()

    private val displayTypeProperty = gameSettings.displayTypeSubject.toPropertyCached()
    private val isWallDisplay = displayTypeProperty.isEqualTo(GameSettings.DisplayType.wall)

    override val root = form {
        fieldset("Game Display Type") {
            field("Type") { enumComboBox(displayTypeProperty) }
        }

        fieldset("Overlay") {
            visibleWhen { isWallDisplay }
            overlaySettings("MetaTag", gameWallSettings.metaTagOverlay)
            overlaySettings("Version", gameWallSettings.versionOverlay)
        }
        fieldset("Cell") {
            setId(Style.gameWallSettings)
            visibleWhen { isWallDisplay }
            cellSettings()
        }
    }

    // TODO: Configure overlay color.
    private fun Fieldset.overlaySettings(name: String, settings: GameWallSettings.OverlaySettingsAccessor) =
        field("Display $name Overlay") {
            hbox(spacing = 5.0) {
                alignment = Pos.CENTER_LEFT
                val isShowProperty = settings.isShowSubject.toPropertyCached()
                jfxToggleButton(isShowProperty)
                labeled("Position") { enumComboBox(settings.positionSubject.toPropertyCached()) }.apply { showWhen { isShowProperty } }
                jfxToggleButton(settings.fillWidthSubject.toPropertyCached()) {
                    showWhen { isShowProperty }
                    text = "Fill Width"
                }
            }
        }

    private fun Fieldset.cellSettings() {
        field("Fixed Size") {
            hbox(spacing = 5.0) {
                alignment = Pos.CENTER_LEFT
                jfxToggleButton(gameWallSettings.cell.isFixedSizeSubject.toPropertyCached())
                labeled("Thumbnail") { enumComboBox(gameWallSettings.cell.imageDisplayTypeSubject.toPropertyCached()) }.apply {
                    showWhen { gameWallSettings.cell.isFixedSizeSubject.toPropertyCached() }
                }
            }
        }
        field("Border") { jfxToggleButton(gameWallSettings.cell.isShowBorderSubject.toPropertyCached()) }
        field("Width") { adjustableTextField(gameWallSettings.cell.widthSubject.toPropertyCached(), "cell width", min = 20.0, max = 500.0) }
        field("Height") { adjustableTextField(gameWallSettings.cell.heightSubject.toPropertyCached(), "cell height", min = 20.0, max = 500.0) }
        field("Horizontal Spacing") { adjustableTextField(gameWallSettings.cell.horizontalSpacingSubject.toPropertyCached(), "horizontal spacing", min = 0.0, max = 100.0) }
        field("Vertical Spacing") { adjustableTextField(gameWallSettings.cell.verticalSpacingSubject.toPropertyCached(), "vertical spacing", min = 0.0, max = 100.0) }
    }

    class Style : Stylesheet() {
        companion object {
            val gameWallSettings by cssid()

            init {
                importStylesheet(Style::class)
            }
        }

        init {
            gameWallSettings {
                maxWidth = 400.px
            }
        }
    }
}