/****************************************************************************
 * Copyright (C) 2016-2019 Yevgeny Krasik                                   *
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

import com.gitlab.ykrasik.gamedex.app.api.settings.*
import com.gitlab.ykrasik.gamedex.app.api.task.ViewWithRunningTask
import com.gitlab.ykrasik.gamedex.app.api.util.channel
import com.gitlab.ykrasik.gamedex.app.javafx.ViewManager
import com.gitlab.ykrasik.gamedex.app.javafx.common.JavaFxCommonOps
import com.gitlab.ykrasik.gamedex.javafx.areYouSureDialog
import com.gitlab.ykrasik.gamedex.javafx.callOnDock
import com.gitlab.ykrasik.gamedex.javafx.callOnUndock
import com.gitlab.ykrasik.gamedex.javafx.control.*
import com.gitlab.ykrasik.gamedex.javafx.theme.GameDexStyle
import com.gitlab.ykrasik.gamedex.javafx.theme.Icons
import com.gitlab.ykrasik.gamedex.javafx.theme.header
import com.gitlab.ykrasik.gamedex.javafx.theme.resetToDefaultButton
import com.gitlab.ykrasik.gamedex.javafx.view.ConfirmationWindow
import com.jfoenix.controls.JFXToggleNode
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.Tab
import tornadofx.*

/**
 * User: ykrasik
 * Date: 06/01/2017
 * Time: 22:22
 */
class JavaFxSettingsView : ConfirmationWindow("Settings", Icons.settings),
    SettingsView,
    ViewWithRunningTask,
    ViewCanChangeGameWallDisplaySettings,
    ViewCanChangeNameOverlayDisplaySettings,
    ViewCanChangeMetaTagOverlayDisplaySettings,
    ViewCanChangeVersionOverlayDisplaySettings {

    private val viewManager: ViewManager by inject()
    private val commonOps: JavaFxCommonOps by di()

    override val resetDefaultsActions = channel<Unit>()

    override val mutableGameWallDisplaySettings = JavaFxGameWallDisplaySettings()
    override val mutableNameOverlayDisplaySettings = JavaFxOverlayDisplaySettings()
    override val mutableMetaTagOverlayDisplaySettings = JavaFxOverlayDisplaySettings()
    override val mutableVersionOverlayDisplaySettings = JavaFxOverlayDisplaySettings()

    private val viewProperty = "view"
    private val tabProperty = "tab"

    private val tabPane = jfxTabPane {
        addClass(GameDexStyle.hiddenTabPaneHeader)
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
        prefWidth = 1000.0
        top = confirmationToolbar {
            gap()
            centeredWindowHeader {
                resetToDefaultButton {
                    action(resetDefaultsActions)
                    stackpaneConstraints { alignment = Pos.CENTER_LEFT }
                }
            }
            gap()
//            spacer()
//
//            label("Window Opacity")
//            percentSlider(windowOpacityProperty, min = 0.2, conflateValueChanges = false) {
//                indicatorPosition = JFXSlider.IndicatorPosition.RIGHT
//                tooltip("Hold 'shift' to hide temporarily.")
//            }
//
//            spacer()
        }
        left = vbox(spacing = 5) {
            paddingAll = 10
            togglegroup {
                var attemptedDeselection = false
                selectedToggleProperty().addListener { _, oldValue, newValue ->
                    // Disallow de-selection.
                    if (newValue == null) {
                        attemptedDeselection = true
                        selectToggle(oldValue)
                    } else {
                        if (!attemptedDeselection) {
                            if (oldValue != null) {
                                (oldValue.properties[viewProperty] as UIComponent).callOnUndock()
                            }
                            (newValue.properties[viewProperty] as UIComponent).callOnDock()
                            tabPane.selectionModel.select(newValue.properties[tabProperty] as Tab)
                        }
                        attemptedDeselection = false
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
                commonOps.providers.forEach { provider ->
                    val icon = commonOps.providerLogo(provider.id).toImageView(height = 30, width = 30)
                    val view = JavaFxProviderSettingsView(provider, icon)
                    entry(view)
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

    private inline fun Node.entry(component: UIComponent, op: JFXToggleNode.() -> Unit = {}) {
        val icon = component.icon
        val tab = tabPane.tab(component)
        tab.graphic = null
        jfxToggleNode(component.title, icon) {
            addClass(GameDexStyle.toolbarButton)
            useMaxWidth = true
            properties += viewProperty to component
            properties += tabProperty to tab
            op()
        }
    }

    override fun confirmResetDefaults() = areYouSureDialog("Reset all settings to default?")

    override fun hide() {
        viewManager.hide(this)
    }
}