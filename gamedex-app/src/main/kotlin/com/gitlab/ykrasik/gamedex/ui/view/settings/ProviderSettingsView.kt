package com.gitlab.ykrasik.gamedex.ui.view.settings

import com.gitlab.ykrasik.gamedex.repository.GameProviderRepository
import com.gitlab.ykrasik.gamedex.repository.logoImage
import com.gitlab.ykrasik.gamedex.ui.jfxToggleNode
import com.gitlab.ykrasik.gamedex.ui.perform
import com.gitlab.ykrasik.gamedex.ui.theme.Theme
import com.gitlab.ykrasik.gamedex.ui.toImageView
import javafx.geometry.Pos
import javafx.scene.control.ToggleGroup
import tornadofx.*

/**
 * User: ykrasik
 * Date: 05/06/2017
 * Time: 15:17
 */
class ProviderSettingsView : View("Provider Settings", Theme.Icon.settings()) {
    private val providerRepository: GameProviderRepository by di()

    private val providerOrderView: ProviderOrderSettingsView by inject()

    private val selection = ToggleGroup().apply {
        // Disallow de-selection.
        selectedToggleProperty().addListener { _, oldValue, newValue ->
            if (newValue == null) {
                selectToggle(oldValue)
            }
        }
    }

    override val root = vbox {
        hbox(spacing = 5.0) {
            jfxToggleNode(group = selection) {
                graphic = hbox(spacing = 5.0) {
                    alignment = Pos.CENTER_LEFT
                    children += Theme.Icon.settings()
                    stackpane {
                        maxWidth = 100.0
                        maxHeight = 50.0
                        minWidth = maxWidth
                        minHeight = maxHeight
                        label("Order")
                    }
                }
                userData = providerOrderView
                isSelected = true
            }
            providerRepository.allProviders.forEach { provider ->
                jfxToggleNode(group = selection) {
                    graphic = hbox(spacing = 5.0) {
                        alignment = Pos.CENTER_LEFT
                        children += Theme.Icon.settings()
                        stackpane {
                            maxHeight = 50.0
                            maxWidth = 100.0
                            minHeight = maxHeight
                            minWidth = maxWidth
                            children += provider.logoImage.toImageView(height = 50.0, width = 100.0)
                        }
                    }
                    userData = ProviderUserSettingsFragment(provider)
                    isSelected = false
                }
            }
        }
        separator {
            paddingTop = 2.0
        }
        stackpane {
            selection.selectedToggleProperty().perform { selected ->
                replaceChildren {
                    children += (selected.userData as UIComponent).root
                }
            }
        }
    }
}