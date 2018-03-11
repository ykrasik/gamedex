package com.gitlab.ykrasik.gamedex.ui.view.settings

import com.gitlab.ykrasik.gamedex.repository.GameProviderRepository
import com.gitlab.ykrasik.gamedex.repository.logoImage
import com.gitlab.ykrasik.gamedex.ui.jfxToggleNode
import com.gitlab.ykrasik.gamedex.ui.theme.CommonStyle
import com.gitlab.ykrasik.gamedex.ui.theme.acceptButton
import com.gitlab.ykrasik.gamedex.ui.theme.cancelButton
import com.gitlab.ykrasik.gamedex.ui.toImageView
import com.gitlab.ykrasik.gamedex.ui.verticalSeparator
import javafx.scene.control.Tab
import javafx.scene.control.TabPane
import javafx.scene.text.FontWeight
import javafx.stage.Modality
import tornadofx.*

/**
 * User: ykrasik
 * Date: 06/01/2017
 * Time: 22:22
 */
// TODO: This looks more like a Screen
class SettingsFragment : Fragment("Settings") {
    private val generalSettingsView: GeneralSettingsView by inject()
    private val gameSettingsView: GameSettingsView by inject()
    private val providerOrderView: ProviderOrderSettingsView by inject()

    private val providerRepository: GameProviderRepository by di()

    private var tabPane: TabPane by singleAssign()

    private var accept = false

    override val root = borderpane {
        top {
            toolbar {
                acceptButton { setOnAction { accept() } }
                verticalSeparator()
                spacer()
                verticalSeparator()
                cancelButton { setOnAction { close() } }
            }
        }
        center {
            tabPane = tabpane { addClass(CommonStyle.hiddenTabPaneHeader) }
        }
        left {
            hbox(spacing = 5.0) {
                paddingAll = 5.0
                vbox(spacing = 5.0) {
                    togglegroup {
                        selectedToggleProperty().addListener { _, oldValue, newValue ->
                            // Disallow de-selection.
                            if (newValue == null) {
                                selectToggle(oldValue)
                            } else {
                                tabPane.selectionModel.select(newValue.userData as Tab)
                            }
                        }
                        jfxToggleNode(generalSettingsView.title, generalSettingsView.icon) {
                            addClass(CommonStyle.fillAvailableWidth)
                            userData = tabPane.tab(generalSettingsView)
                            isSelected = true
                        }
                        separator()
                        jfxToggleNode(gameSettingsView.title, gameSettingsView.icon) {
                            addClass(CommonStyle.fillAvailableWidth)
                            userData = tabPane.tab(gameSettingsView)
                        }
                        separator()
                        label("Provider") { addClass(Style.navigationLabel) }
                        jfxToggleNode(providerOrderView.title, providerOrderView.icon) {
                            addClass(CommonStyle.fillAvailableWidth)
                            userData = tabPane.tab(providerOrderView)
                        }
                        providerRepository.allProviders.forEach { provider ->
                            jfxToggleNode {
                                addClass(CommonStyle.fillAvailableWidth)
                                graphic = hbox {
                                    addClass(CommonStyle.jfxToggleNodeLabel, CommonStyle.fillAvailableWidth)
                                    children += provider.logoImage.toImageView {
                                        fitHeight = 28.0
                                        isPreserveRatio = true
                                    }
                                    spacer {
                                        paddingLeft = 5.0
                                        paddingRight = 5.0
                                    }
                                    label(provider.id)
                                }
                                userData = tabPane.tab(ProviderUserSettingsFragment(provider))
                            }
                        }
                        separator()
                    }
                }
                verticalSeparator(padding = 0.0)
            }
        }
    }

    fun show(): Boolean {
        openWindow(block = true, modality = Modality.WINDOW_MODAL)
        return accept
    }

    private fun accept() {
        accept = true
        close()
    }

    class Style : Stylesheet() {
        companion object {
            val navigationLabel by cssclass()

            init {
                importStylesheet(Style::class)
            }
        }

        init {
            navigationLabel {
                fontSize = 18.px
                fontWeight = FontWeight.BOLD
            }
        }
    }
}