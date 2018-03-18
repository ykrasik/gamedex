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

import com.gitlab.ykrasik.gamedex.repository.GameProviderRepository
import com.gitlab.ykrasik.gamedex.repository.logoImage
import com.gitlab.ykrasik.gamedex.javafx.jfxToggleNode
import com.gitlab.ykrasik.gamedex.javafx.CommonStyle
import com.gitlab.ykrasik.gamedex.javafx.acceptButton
import com.gitlab.ykrasik.gamedex.javafx.cancelButton
import com.gitlab.ykrasik.gamedex.javafx.toImageView
import com.gitlab.ykrasik.gamedex.javafx.verticalSeparator
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
                acceptButton {
                    isDefaultButton = true
                    setOnAction { accept() }
                }
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
                        separator {
                            paddingTop = 10.0
                            paddingBottom = 10.0
                        }
                        label("Providers") { addClass(Style.navigationLabel) }
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
                fontSize = 14.px
                fontWeight = FontWeight.BOLD
            }
        }
    }
}