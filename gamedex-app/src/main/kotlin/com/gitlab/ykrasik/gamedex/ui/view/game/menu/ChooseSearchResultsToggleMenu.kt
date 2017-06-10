package com.gitlab.ykrasik.gamedex.ui.view.game.menu

import com.gitlab.ykrasik.gamedex.settings.GameSettings
import com.gitlab.ykrasik.gamedex.ui.jfxToggleNode
import com.gitlab.ykrasik.gamedex.ui.theme.CommonStyle
import javafx.scene.layout.VBox
import tornadofx.*

/**
 * User: ykrasik
 * Date: 10/06/2017
 * Time: 21:15
 */
class ChooseSearchResultsToggleMenu : Fragment() {
    private val settings: GameSettings by di()

    override val root = vbox(spacing = 5.0) {
        togglegroup {
            GameSettings.ChooseResults.values().forEach { chooseResults ->
                jfxToggleNode(chooseResults.key) {
                    addClass(CommonStyle.fillAvailableWidth)
                    isSelected = settings.chooseResults == chooseResults
                    selectedProperty().onChange {
                        if (it) settings.chooseResults = chooseResults
                    }
                    settings.chooseResultsProperty.onChange {
                        isSelected = chooseResults == it
                    }
                }
            }
        }
    }

    fun install(vbox: VBox) {
        vbox.children += root
    }
}