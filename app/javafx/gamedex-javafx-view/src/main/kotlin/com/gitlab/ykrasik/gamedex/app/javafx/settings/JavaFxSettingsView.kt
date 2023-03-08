/****************************************************************************
 * Copyright (C) 2016-2023 Yevgeny Krasik                                   *
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
import com.gitlab.ykrasik.gamedex.app.api.util.broadcastFlow
import com.gitlab.ykrasik.gamedex.app.javafx.JavaFxViewManager
import com.gitlab.ykrasik.gamedex.app.javafx.common.JavaFxCommonOps
import com.gitlab.ykrasik.gamedex.javafx.callOnDock
import com.gitlab.ykrasik.gamedex.javafx.callOnUndock
import com.gitlab.ykrasik.gamedex.javafx.control.jfxTabPane
import com.gitlab.ykrasik.gamedex.javafx.control.jfxToggleNode
import com.gitlab.ykrasik.gamedex.javafx.control.toImageView
import com.gitlab.ykrasik.gamedex.javafx.control.verticalGap
import com.gitlab.ykrasik.gamedex.javafx.theme.GameDexStyle
import com.gitlab.ykrasik.gamedex.javafx.theme.Icons
import com.gitlab.ykrasik.gamedex.javafx.theme.header
import com.gitlab.ykrasik.gamedex.javafx.theme.resetToDefaultButton
import com.gitlab.ykrasik.gamedex.javafx.view.ConfirmationWindow
import com.jfoenix.controls.JFXToggleNode
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.Tab
import javafx.scene.control.Toggle
import javafx.scene.control.ToggleGroup
import tornadofx.*

/**
 * User: ykrasik
 * Date: 06/01/2017
 * Time: 22:22
 */
class JavaFxSettingsView : ConfirmationWindow("Settings", Icons.settings),
    SettingsView,
    ViewCanChangeGameWallDisplaySettings,
    ViewCanChangeNameOverlayDisplaySettings,
    ViewCanChangeMetaTagOverlayDisplaySettings,
    ViewCanChangeVersionOverlayDisplaySettings {

    private val viewManager: JavaFxViewManager by inject()

    private val commonOps: JavaFxCommonOps by di()

    override val resetDefaultsActions = broadcastFlow<Unit>()

    override val mutableGameWallDisplaySettings = JavaFxMutableGameWallDisplaySettings()
    override val mutableNameOverlayDisplaySettings = JavaFxMutableOverlayDisplaySettings("name")
    override val mutableMetaTagOverlayDisplaySettings = JavaFxMutableOverlayDisplaySettings("metaTag")
    override val mutableVersionOverlayDisplaySettings = JavaFxMutableOverlayDisplaySettings("version")

    private val tabPane = jfxTabPane {
        addClass(GameDexStyle.hiddenTabPaneHeader)
        paddingAll = 5
    }

    init {
        register()
    }

    private val generalSettingsView: JavaFxGeneralSettingsView by inject()
    private val wallDisplaySettingsView = JavaFxWallDisplaySettingsView(mutableGameWallDisplaySettings)
    private val nameDisplaySettingsView = JavaFxGameDisplaySettingsView(mutableNameOverlayDisplaySettings, "Name")
    private val metaTagDisplaySettingsView = JavaFxGameDisplaySettingsView(mutableMetaTagOverlayDisplaySettings, "MetaTag")
    private val versionDisplaySettingsView = JavaFxGameDisplaySettingsView(mutableVersionOverlayDisplaySettings, "Version")
    private val providerOrderView: JavaFxProviderOrderSettingsView by inject()

    private var toggleGroup: ToggleGroup by singleAssign()

    override val root = borderpane {
        prefWidth = 1000.0
        top = confirmationToolbar {
            centeredWindowHeader {
                resetToDefaultButton {
                    action(resetDefaultsActions)
                    stackpaneConstraints { alignment = Pos.CENTER_LEFT }
                }
            }
        }
        left = vbox(spacing = 5) {
            paddingAll = 10
            usePrefSize = true
            toggleGroup = togglegroup {
                header("General")
                entry(generalSettingsView)

                verticalGap(size = 20)

                header("Display")
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
    }

    init {
        var attemptedDeselection = false
        toggleGroup.selectedToggleProperty().addListener { _, oldValue, newValue ->
            // Disallow de-selection.
            if (newValue == null) {
                attemptedDeselection = true
                toggleGroup.selectToggle(oldValue)
            } else {
                if (!attemptedDeselection) {
                    oldValue?.component?.callOnUndock()
                    newValue.component.callOnDock()
                    tabPane.selectionModel.select(newValue.tab)
                }
                attemptedDeselection = false
            }
        }

        whenDocked {
            if (toggleGroup.selectedToggle == null) {
                toggleGroup.toggles.first().isSelected = true
            } else {
                toggleGroup.selectedToggle.component.callOnDock()
            }
        }
        whenUndocked {
            toggleGroup.selectedToggle.component.callOnUndock()
        }
    }

    private inline fun Node.entry(component: UIComponent, op: JFXToggleNode.() -> Unit = {}): JFXToggleNode {
        val icon = component.icon
        val tab = tabPane.tab(component)
        tab.graphic = null
        return jfxToggleNode(component.title, icon) {
            addClass(GameDexStyle.toolbarButton)
            useMaxWidth = true
            this.component = component
            this.tab = tab
            op()
        }
    }

    override suspend fun confirmResetDefaults() = viewManager.showAreYouSureDialog("Reset all settings to default?", Icons.warning)

    private var Toggle.component: UIComponent
        get() = properties["gameDex.component"] as UIComponent
        set(value) {
            properties["gameDex.component"] = value
        }

    private var Toggle.tab: Tab
        get() = properties["gameDex.tab"] as Tab
        set(value) {
            properties["gameDex.tab"] = value
        }
}