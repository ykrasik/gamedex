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
import com.gitlab.ykrasik.gamedex.javafx.control.*
import com.gitlab.ykrasik.gamedex.javafx.view.ConfirmationWindow
import com.gitlab.ykrasik.gamedex.provider.GameProvider
import com.gitlab.ykrasik.gamedex.provider.ProviderId
import com.jfoenix.controls.JFXSlider
import com.jfoenix.controls.JFXToggleNode
import javafx.beans.property.ReadOnlyObjectProperty
import javafx.scene.Node
import javafx.scene.control.Tab
import javafx.scene.control.Toggle
import tornadofx.*

/**
 * User: ykrasik
 * Date: 06/01/2017
 * Time: 22:22
 */
class JavaFxSettingsView : ConfirmationWindow("Settings", Icons.settings),
    SettingsView,
    ViewWithProviderLogos,
    ViewWithRunningTask,
    ViewCanChangeGameWallDisplaySettings,
    ViewCanChangeNameOverlayDisplaySettings,
    ViewCanChangeMetaTagOverlayDisplaySettings,
    ViewCanChangeVersionOverlayDisplaySettings {

    override val providers = mutableListOf<GameProvider>()
    override var providerLogos = emptyMap<ProviderId, Image>()

    override val resetDefaultsActions = channel<Unit>()

    override val mutableGameWallDisplaySettings = JavaFxGameWallDisplaySettings()
    override val mutableNameOverlayDisplaySettings = JavaFxOverlayDisplaySettings()
    override val mutableMetaTagOverlayDisplaySettings = JavaFxOverlayDisplaySettings()
    override val mutableVersionOverlayDisplaySettings = JavaFxOverlayDisplaySettings()

    private var selectedToggleProperty: ReadOnlyObjectProperty<Toggle> by singleAssign()

    private val viewProperty = "view"
    private val tabProperty = "tab"

    private val tabPane = jfxTabPane {
        addClass(CommonStyle.hiddenTabPaneHeader)
        paddingAll = 5
    }

    init {
        register()
    }

    private val wallDisplaySettingsView = JavaFxWallDisplaySettingsView(mutableGameWallDisplaySettings)
    private val nameDisplaySettingsView = JavaFxGameDisplaySettingsView(mutableNameOverlayDisplaySettings, "Name")
    private val metaTagDisplaySettingsView = JavaFxGameDisplaySettingsView(mutableMetaTagOverlayDisplaySettings, "MetaTag")
    private val versionDisplaySettingsView = JavaFxGameDisplaySettingsView(mutableVersionOverlayDisplaySettings, "Version")
    private val providerOrderView: JavaFxProviderOrderSettingsView by inject()

    override val root = borderpane {
//        prefHeight = screenBounds.height / 2
        prefWidth = 800.0
        top = confirmationToolbar {
            gap()
            resetToDefaultButton { eventOnAction(resetDefaultsActions) }
            spacer()

            // TODO: This is probably a sucky way of doing this, find a better solution.
            label("Window Opacity")
            percentSlider(windowOpacityProperty, min = 0.2, conflateValueChanges = false) {
                indicatorPosition = JFXSlider.IndicatorPosition.RIGHT
                tooltip("Hold 'shift' to hide temporarily.")
            }

            spacer()
        }
        left = vbox(spacing = 5) {
            paddingAll = 10
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
        center = tabPane

        var hidden = false
        var prevOpacity = 1.0
        setOnKeyPressed { e ->
            if (e.isShiftDown) {
                prevOpacity = windowOpacity
                windowOpacity = 0.0
                hidden = true
            }
        }
        setOnKeyReleased {
            if (hidden) {
                windowOpacity = prevOpacity
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

    override fun confirmResetDefaults() = areYouSureDialog("Reset all settings to default?")
}