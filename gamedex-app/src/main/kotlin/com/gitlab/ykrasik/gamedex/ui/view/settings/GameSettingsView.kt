package com.gitlab.ykrasik.gamedex.ui.view.settings

import com.gitlab.ykrasik.gamedex.settings.AllSettings
import com.gitlab.ykrasik.gamedex.settings.GameSettings
import com.gitlab.ykrasik.gamedex.ui.enumComboBox
import com.gitlab.ykrasik.gamedex.ui.theme.Theme
import com.gitlab.ykrasik.gamedex.ui.widgets.adjustableTextField
import tornadofx.*

/**
 * User: ykrasik
 * Date: 05/06/2017
 * Time: 15:21
 */
class GameSettingsView : View("Game Settings", Theme.Icon.games()) {
    private val settings: AllSettings by di()

    override val root = form {
        fieldset("Game Display Type") {
            field("Type") { enumComboBox(settings.game.displayTypeProperty) }
        }

        fieldset("Game Wall") {
            setId(Style.gameWallSettings)
            visibleWhen { settings.game.displayTypeProperty.isEqualTo(GameSettings.DisplayType.wall) }
            field("Cell Image Display") { enumComboBox(settings.gameWall.imageDisplayTypeProperty) }
            field("Cell Width") {
                adjustableTextField(settings.gameWall.cellWidthProperty, "cell width", min = 20.0, max = 500.0)
            }
            field("Cell Height") {
                adjustableTextField(settings.gameWall.cellHeightProperty, "cell height", min = 20.0, max = 500.0)
            }
            field("Cell Horizontal Spacing") {
                adjustableTextField(settings.gameWall.cellHorizontalSpacingProperty, "horizontal spacing", min = 0.0, max = 100.0)
            }
            field("Cell Vertical Spacing") {
                adjustableTextField(settings.gameWall.cellVerticalSpacingProperty, "vertical spacing", min = 0.0, max = 100.0)
            }
        }
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
                maxWidth = 300.px
            }
        }
    }
}