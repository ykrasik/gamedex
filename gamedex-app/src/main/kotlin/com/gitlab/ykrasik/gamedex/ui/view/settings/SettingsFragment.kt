package com.gitlab.ykrasik.gamedex.ui.view.settings

import com.gitlab.ykrasik.gamedex.ui.theme.CommonStyle
import com.gitlab.ykrasik.gamedex.ui.theme.acceptButton
import com.gitlab.ykrasik.gamedex.ui.theme.toolbarButton
import com.gitlab.ykrasik.gamedex.ui.verticalSeparator
import javafx.scene.control.TabPane
import javafx.scene.layout.Region
import tornadofx.*


/**
 * User: ykrasik
 * Date: 06/01/2017
 * Time: 22:22
 */
class SettingsFragment : Fragment("Settings") {
    private val generalSettingsView: GeneralSettingsView by inject()
    private val providerSettingsView: ProviderSettingsView by inject()
    private val gameSettingsView: GameSettingsView by inject()

    private var tabPane: TabPane by singleAssign()

    override val root = borderpane {
        minWidth = 700.0
        center {
            tabPane = tabpane {
                addClass(CommonStyle.tabbedNavigation)
                settingsTab(generalSettingsView)
                settingsTab(providerSettingsView)
                settingsTab(gameSettingsView)
            }
        }
        top {
            toolbar {
                minWidth = Region.USE_COMPUTED_SIZE
                acceptButton { setOnAction { close() } }
                verticalSeparator()
                spacer()
                verticalSeparator()
                tabPane.tabs.forEach { tab ->
                    toolbarButton(tab.text, tab.graphic) {
                        minWidth = Region.USE_COMPUTED_SIZE
                        setOnAction { tabPane.selectionModel.select(tab) }
                    }
                    verticalSeparator()
                }
            }
        }
    }

    private fun TabPane.settingsTab(content: View) = tab(content) {
        addClass(Style.settingsTab)
        graphic = content.icon
    }

    fun show() {
        openModal(block = true)
    }

    class Style : Stylesheet() {
        companion object {
            val settingsTab by cssclass()

            init {
                importStylesheet(Style::class)
            }
        }

        init {
            settingsTab {
                padding = box(40.px)
            }
        }
    }
}