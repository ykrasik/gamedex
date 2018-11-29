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
import com.gitlab.ykrasik.gamedex.app.api.image.ViewWithProviderLogos
import com.gitlab.ykrasik.gamedex.app.api.settings.*
import com.gitlab.ykrasik.gamedex.app.api.task.ViewWithRunningTask
import com.gitlab.ykrasik.gamedex.app.api.util.channel
import com.gitlab.ykrasik.gamedex.app.javafx.image.image
import com.gitlab.ykrasik.gamedex.javafx.*
import com.gitlab.ykrasik.gamedex.javafx.view.PresentableView
import com.gitlab.ykrasik.gamedex.provider.GameProvider
import com.gitlab.ykrasik.gamedex.provider.ProviderId
import com.jfoenix.controls.JFXToggleNode
import javafx.beans.property.ReadOnlyObjectProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleDoubleProperty
import javafx.scene.Node
import javafx.scene.control.Tab
import javafx.scene.control.TabPane
import javafx.scene.control.Toggle
import tornadofx.*

/**
 * User: ykrasik
 * Date: 06/01/2017
 * Time: 22:22
 */
class JavaFxSettingsView : PresentableView("Settings"),
    SettingsView,
    ViewWithProviderLogos,
    ViewWithRunningTask,
    ViewCanChangeGameWallDisplaySettings,
    ViewCanChangeNameOverlayDisplaySettings,
    ViewCanChangeMetaTagOverlayDisplaySettings,
    ViewCanChangeVersionOverlayDisplaySettings {

    override val providers = mutableListOf<GameProvider>()
    override var providerLogos = emptyMap<ProviderId, Image>()

    override val acceptActions = channel<Unit>()
    override val resetDefaultsActions = channel<Unit>()
    override val cancelActions = channel<Unit>()

    private val runningTaskProperty = SimpleBooleanProperty(false)
    override var runningTask by runningTaskProperty

    override val mutableGameWallDisplaySettings = JavaFxGameWallDisplaySettings()
    override val mutableNameOverlayDisplaySettings = JavaFxOverlayDisplaySettings()
    override val mutableMetaTagOverlayDisplaySettings = JavaFxOverlayDisplaySettings()
    override val mutableVersionOverlayDisplaySettings = JavaFxOverlayDisplaySettings()

    private var tabPane: TabPane by singleAssign()

    private var selectedToggleProperty: ReadOnlyObjectProperty<Toggle> by singleAssign()

    private val windowOpacity = SimpleDoubleProperty(1.0)

    private val viewProperty = "view"
    private val tabProperty = "tab"

    init {
        viewRegistry.onCreate(this)

        var prevOpacity = 1.0
        runningTaskProperty.onChange { isRunningTask ->
            val showing = modalStage?.isShowing ?: false
            if (showing) {
                // Make the settings window invisible while running any task.
                if (isRunningTask) {
                    prevOpacity = windowOpacity.value
                    windowOpacity.value = 0.0
                } else {
                    windowOpacity.value = prevOpacity
                }
            }
        }
    }

    private val databaseSettingsView: JavaFxDatabaseSettingsView by inject()
    private val wallDisplaySettingsView = JavaFxWallDisplaySettingsView(mutableGameWallDisplaySettings)
    private val nameDisplaySettingsView = JavaFxGameDisplaySettingsView(mutableNameOverlayDisplaySettings, "Name")
    private val metaTagDisplaySettingsView = JavaFxGameDisplaySettingsView(mutableMetaTagOverlayDisplaySettings, "MetaTag")
    private val versionDisplaySettingsView = JavaFxGameDisplaySettingsView(mutableVersionOverlayDisplaySettings, "Version")
    private val providerOrderView: JavaFxProviderOrderSettingsView by inject()

    override val root = borderpane {
        prefHeight = screenBounds.height / 2
        prefWidth = 800.0
        top {
            toolbar {
                cancelButton { eventOnAction(cancelActions) }
                gap()
                resetToDefaultButton { eventOnAction(resetDefaultsActions) }
                spacer()
                percentSlider(windowOpacity, min = 0.2, max = 1, text = "Opacity") {
                    tooltip("Hold 'alt' to hide temporarily.")
                }
                spacer()
                acceptButton { eventOnAction(acceptActions) }
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
                        header("General")
                        entry(databaseSettingsView).apply { isSelected = true }
                        verticalGap(size = 20)

                        header("Game Display")
                        entry(wallDisplaySettingsView)
                        entry(nameDisplaySettingsView)
                        entry(metaTagDisplaySettingsView)
                        entry(versionDisplaySettingsView)
                        verticalGap(size = 20)

                        header("Providers")
                        entry(providerOrderView)
                        providers.forEach { provider ->
                            val view = JavaFxProviderSettingsView(provider)
                            val tab = tabPane.tab(view)
                            jfxToggleNode(provider.id, graphic = providerLogos[provider.id]!!.image.toImageView(height = 36, width = 36)) {
                                addClass(CommonStyle.toolbarButton)
                                useMaxWidth = true
                                properties += viewProperty to view
                                properties += tabProperty to tab
                            }
                        }
                    }
                }
                gap()
            }
        }

        var hidden = false
        var prevOpacity = 1.0
        setOnKeyPressed { e ->
            if (e.isAltDown) {
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

    private fun Node.entry(component: UIComponent, op: JFXToggleNode.() -> Unit = {}) = jfxToggleNode(component.title, component.icon) {
        useMaxWidth = true
        properties += viewProperty to component
        properties += tabProperty to tabPane.tab(component)
        op()
    }

    override fun onDock() {
        currentStage!!.opacityProperty().cleanBind(windowOpacity)
    }

    override fun confirmResetDefaults() = areYouSureDialog("Reset all settings to default?")
}