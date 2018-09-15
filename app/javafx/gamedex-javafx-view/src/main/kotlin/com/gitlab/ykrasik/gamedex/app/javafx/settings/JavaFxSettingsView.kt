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

package com.gitlab.ykrasik.gamedex.app.javafx.settings

import com.gitlab.ykrasik.gamedex.app.api.image.Image
import com.gitlab.ykrasik.gamedex.app.api.settings.SettingsView
import com.gitlab.ykrasik.gamedex.app.api.util.channel
import com.gitlab.ykrasik.gamedex.app.javafx.image.image
import com.gitlab.ykrasik.gamedex.javafx.*
import com.gitlab.ykrasik.gamedex.javafx.dialog.areYouSureDialog
import com.gitlab.ykrasik.gamedex.javafx.view.PresentableTabView
import com.gitlab.ykrasik.gamedex.javafx.view.PresentableView
import com.gitlab.ykrasik.gamedex.provider.GameProvider
import com.gitlab.ykrasik.gamedex.provider.ProviderId
import com.jfoenix.controls.JFXToggleNode
import javafx.beans.property.ReadOnlyObjectProperty
import javafx.beans.property.SimpleDoubleProperty
import javafx.scene.Node
import javafx.scene.control.Tab
import javafx.scene.control.TabPane
import javafx.scene.control.Toggle
import javafx.scene.text.FontWeight
import tornadofx.*

/**
 * User: ykrasik
 * Date: 06/01/2017
 * Time: 22:22
 */
class JavaFxSettingsView : PresentableView("Settings"), SettingsView {
    private val generalSettingsView: JavaFxGeneralSettingsView by inject()
    private val gameDisplaySettingsView: JavaFxGameDisplaySettingsView by inject()
    private val providerOrderView: JavaFxProviderOrderSettingsView by inject()

    override val providers = mutableListOf<GameProvider>()
    override var providerLogos = emptyMap<ProviderId, Image>()

    override val acceptActions = channel<Unit>()
    override val resetDefaultsActions = channel<Unit>()
    override val cancelActions = channel<Unit>()

    private var tabPane: TabPane by singleAssign()

    private var selectedToggleProperty: ReadOnlyObjectProperty<Toggle> by singleAssign()

    private val windowOpacity = SimpleDoubleProperty(1.0)

    private val viewProperty = "view"
    private val tabProperty = "tab"

    init {
        viewRegistry.register(this)
    }

    override val root = borderpane {
        prefHeight = screenBounds.height * 3 / 4
        top {
            toolbar {
                acceptButton { eventOnAction(acceptActions) }
                verticalSeparator()
                spacer()
                verticalSeparator()
                percentSlider(windowOpacity, min = 0.2, max = 1, text = "Opacity") {
                    tooltip("Hold 'ctrl' to hide temporarily.")
                }
                verticalSeparator()
                spacer()
                verticalSeparator()
                resetToDefaultButton("Reset to Defaults") { eventOnAction(resetDefaultsActions) }
                verticalSeparator()
                cancelButton { eventOnAction(cancelActions) }
            }
        }
        center {
            tabPane = jfxTabPane { addClass(CommonStyle.hiddenTabPaneHeader) }
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
                                if (oldValue != null) {
                                    (oldValue.properties[viewProperty] as UIComponent).callOnUndock()
                                }
                                (newValue.properties[viewProperty] as UIComponent).callOnDock()
                                tabPane.selectionModel.select(newValue.properties[tabProperty] as Tab)
                            }
                        }
                        entry(generalSettingsView).apply { isSelected = true }
                        entry(gameDisplaySettingsView)
                        entry(providerOrderView)
                        separator {
                            paddingTop = 10.0
                            paddingBottom = 10.0
                        }
                        label("Providers") { addClass(Style.navigationLabel) }
                        providers.forEach { provider ->
                            val view = JavaFxProviderSettingsView(provider)
                            val tab = tabPane.tab(view)
                            jfxToggleNode(provider.id) {
                                useMaxWidth = true
                                graphic = hbox {
                                    addClass(CommonStyle.jfxToggleNodeLabel)
                                    useMaxWidth = true
                                    imageview(providerLogos[provider.id]!!.image) {
                                        fitHeight = 28.0
                                        isPreserveRatio = true
                                    }
                                    spacer {
                                        paddingLeft = 5.0
                                        paddingRight = 5.0
                                    }
                                    label(provider.id)
                                }
                                properties += viewProperty to view
                                properties += tabProperty to tab
                            }
                        }
                        separator()
                    }
                }
                verticalSeparator(padding = 0.0)
            }
        }

        var hidden = false
        var prevOpacity = 1.0
        setOnKeyPressed { e ->
            if (e.isControlDown) {
                prevOpacity = windowOpacity.value
                windowOpacity.value = 0.0
                hidden = true
            }
        }
        setOnKeyReleased {
            if (hidden) {
                windowOpacity.value = prevOpacity
                hidden = false
            }
        }
    }

    private fun Node.entry(component: PresentableTabView, op: JFXToggleNode.() -> Unit = {}) = jfxToggleNode(component.title, component.icon) {
        useMaxWidth = true
        properties += viewProperty to component
        properties += tabProperty to tabPane.tab(component)
        op()
    }

    override fun onDock() {
        currentStage!!.opacityProperty().cleanBind(windowOpacity)
    }

    override fun confirmResetDefaults() = areYouSureDialog("Reset all settings to default?")

    class Style : Stylesheet() {
        companion object {
            val navigationLabel by cssclass()

            init {
                importStylesheetSafe(Style::class)
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