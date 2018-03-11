package com.gitlab.ykrasik.gamedex.ui.view.settings

import com.gitlab.ykrasik.gamedex.repository.GameProviderRepository
import com.gitlab.ykrasik.gamedex.repository.logoImage
import com.gitlab.ykrasik.gamedex.ui.jfxToggleNode
import com.gitlab.ykrasik.gamedex.ui.theme.CommonStyle
import com.gitlab.ykrasik.gamedex.ui.theme.acceptButton
import com.gitlab.ykrasik.gamedex.ui.theme.cancelButton
import com.gitlab.ykrasik.gamedex.ui.toImageView
import com.gitlab.ykrasik.gamedex.ui.verticalSeparator
import com.jfoenix.controls.JFXToggleNode
import javafx.beans.property.ReadOnlyObjectProperty
import javafx.scene.Node
import javafx.scene.control.Tab
import javafx.scene.control.TabPane
import javafx.scene.control.Toggle
import javafx.scene.text.FontWeight
import javafx.stage.Modality
import tornadofx.*

/**
 * User: ykrasik
 * Date: 06/01/2017
 * Time: 22:22
 */
class SettingsView : View("Settings") {
    private val generalSettingsView: GeneralSettingsView by inject()
    private val gameSettingsView: GameSettingsView by inject()
    private val providerOrderView: ProviderOrderSettingsView by inject()

    private val providerRepository: GameProviderRepository by di()

    private var tabPane: TabPane by singleAssign()

    private var selectedToggleProperty: ReadOnlyObjectProperty<Toggle> by singleAssign()

    private var accept = false
    private val viewProperty = "view"
    private val tabProperty = "tab"

    override val root = borderpane {
        top {
            toolbar {
                acceptButton { setOnAction { accept() } }
                verticalSeparator()
                spacer()
                verticalSeparator()
                cancelButton {
                    isCancelButton = true
                    setOnAction { close() }
                }
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
                        selectedToggleProperty = selectedToggleProperty()
                        selectedToggleProperty().addListener { _, oldValue, newValue ->
                            // Disallow de-selection.
                            if (newValue == null) {
                                selectToggle(oldValue)
                            } else {
                                (newValue.properties[viewProperty] as UIComponent).onDock()
                                tabPane.selectionModel.select(newValue.properties[tabProperty] as Tab)
                            }
                        }
                        entry(generalSettingsView).apply { isSelected = true }
                        entry(gameSettingsView)
                        entry(providerOrderView)
                        providerRepository.allProviders.forEach { provider ->
                            val view = ProviderUserSettingsFragment(provider)
                            entry(view) {
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
                            }
                        }
                        separator()
                    }
                }
                verticalSeparator(padding = 0.0)
            }
        }
    }

    private fun Node.entry(component: UIComponent, op: JFXToggleNode.() -> Unit = {}) = jfxToggleNode(component.title, component.icon) {
        addClass(CommonStyle.fillAvailableWidth)
        properties += viewProperty to component
        properties += tabProperty to tabPane.tab(component)
        op()
    }

    fun show(): Boolean {
        (selectedToggleProperty.value.properties[viewProperty] as UIComponent).onDock()
        openWindow(block = true, modality = Modality.APPLICATION_MODAL)
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