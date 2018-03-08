package com.gitlab.ykrasik.gamedex.ui.view.settings

import com.gitlab.ykrasik.gamedex.settings.GameSettings
import com.gitlab.ykrasik.gamedex.settings.GameWallSettings
import com.gitlab.ykrasik.gamedex.ui.enumComboBox
import com.gitlab.ykrasik.gamedex.ui.jfxToggleButton
import com.gitlab.ykrasik.gamedex.ui.labeled
import com.gitlab.ykrasik.gamedex.ui.showWhen
import com.gitlab.ykrasik.gamedex.ui.theme.Theme
import com.gitlab.ykrasik.gamedex.ui.widgets.adjustableTextField
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

    private val isWallDisplay = gameSettings.displayTypeProperty.isEqualTo(GameSettings.DisplayType.wall)

    override val root = form {
        fieldset("Game Display Type") {
            field("Type") { enumComboBox(gameSettings.displayTypeProperty) }
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
                jfxToggleButton(settings.isShowProperty)
                labeled("Position") { enumComboBox(settings.positionProperty) }.apply { showWhen { settings.isShowProperty } }
                jfxToggleButton(settings.fillWidthProperty) {
                    showWhen { settings.isShowProperty }
                    text = "Fill Width"
                }
            }
        }

    private fun Fieldset.cellSettings() {
        field("Fixed Size") {
            hbox(spacing = 5.0) {
                alignment = Pos.CENTER_LEFT
                jfxToggleButton(gameWallSettings.cell.isFixedSizeProperty)
                labeled("Thumbnail") { enumComboBox(gameWallSettings.cell.imageDisplayTypeProperty) }.apply {
                    showWhen { gameWallSettings.cell.isFixedSizeProperty }
                }
            }
        }
        field("Border") { jfxToggleButton(gameWallSettings.cell.isShowBorderProperty) }
        field("Width") { adjustableTextField(gameWallSettings.cell.widthProperty, "cell width", min = 20.0, max = 500.0) }
        field("Height") { adjustableTextField(gameWallSettings.cell.heightProperty, "cell height", min = 20.0, max = 500.0) }
        field("Horizontal Spacing") { adjustableTextField(gameWallSettings.cell.horizontalSpacingProperty, "horizontal spacing", min = 0.0, max = 100.0) }
        field("Vertical Spacing") { adjustableTextField(gameWallSettings.cell.verticalSpacingProperty, "vertical spacing", min = 0.0, max = 100.0) }
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