package com.gitlab.ykrasik.gamedex.ui.view.settings

import com.gitlab.ykrasik.gamedex.settings.GameSettings
import com.gitlab.ykrasik.gamedex.settings.GameWallSettings
import com.gitlab.ykrasik.gamedex.settings.Settings
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
    private val settings: Settings by di()

    private val isWallDisplay = settings.game.displayTypeProperty.isEqualTo(GameSettings.DisplayType.wall)

    override val root = form {
        fieldset("Game Display Type") {
            field("Type") { enumComboBox(settings.game.displayTypeProperty) }
        }

        fieldset("Overlay") {
            visibleWhen { isWallDisplay }
            overlaySettings("MetaTag", settings.gameWall.metaTagOverlay)
            overlaySettings("Version", settings.gameWall.versionOverlay)
        }
        fieldset("Cell") {
            setId(Style.gameWallSettings)
            visibleWhen { isWallDisplay }
            cellSettings()
        }
    }

    // TODO: Configure overlay color.
    private fun Fieldset.overlaySettings(name: String, settings: GameWallSettings.GameWallOverlaySettings) =
        field("Display $name Overlay") {
            hbox(spacing = 5.0) {
                alignment = Pos.CENTER_LEFT
                jfxToggleButton(settings.isShowProperty)
                labeled("Location") { enumComboBox(settings.locationProperty) }.apply { showWhen { settings.isShowProperty } }
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
                jfxToggleButton(settings.gameWall.cell.isFixedSizeProperty)
                labeled("Thumbnail") { enumComboBox(settings.gameWall.cell.imageDisplayTypeProperty) }.apply {
                    showWhen { settings.gameWall.cell.isFixedSizeProperty }
                }
            }
        }
        field("Border") { jfxToggleButton(settings.gameWall.cell.isShowBorderProperty) }
        field("Width") { adjustableTextField(settings.gameWall.cell.widthProperty, "cell width", min = 20.0, max = 500.0) }
        field("Height") { adjustableTextField(settings.gameWall.cell.heightProperty, "cell height", min = 20.0, max = 500.0) }
        field("Horizontal Spacing") { adjustableTextField(settings.gameWall.cell.horizontalSpacingProperty, "horizontal spacing", min = 0.0, max = 100.0) }
        field("Vertical Spacing") { adjustableTextField(settings.gameWall.cell.verticalSpacingProperty, "vertical spacing", min = 0.0, max = 100.0) }
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