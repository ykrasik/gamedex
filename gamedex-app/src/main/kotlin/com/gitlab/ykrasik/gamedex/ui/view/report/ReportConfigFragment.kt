package com.gitlab.ykrasik.gamedex.ui.view.report

import com.gitlab.ykrasik.gamedex.core.ReportConfig
import com.gitlab.ykrasik.gamedex.ui.perform
import com.gitlab.ykrasik.gamedex.ui.theme.acceptButton
import com.gitlab.ykrasik.gamedex.ui.theme.cancelButton
import com.gitlab.ykrasik.gamedex.ui.verticalSeparator
import javafx.beans.property.SimpleObjectProperty
import javafx.event.EventTarget
import javafx.geometry.Pos
import javafx.scene.text.FontWeight
import tornadofx.*

/**
 * User: ykrasik
 * Date: 17/06/2017
 * Time: 16:56
 */
// TOOD: Add unallowed names
class ReportConfigFragment(initialConfig: ReportConfig) : Fragment("Report Config") {
    private val reportConfigProperty = SimpleObjectProperty(initialConfig)
    private var reportConfig by reportConfigProperty

    private var accept = false

    override val root = borderpane {
        addClass(Style.ruleWindow)
        top {
            toolbar {
                acceptButton { setOnAction { close(accept = true) } }
                verticalSeparator()
                spacer()
                verticalSeparator()
                cancelButton { setOnAction { close(accept = false) } }
            }
        }
        center {
            vbox(spacing = 10) {
                addClass(Style.rulesContent)
                hbox(spacing = 10) {
                    alignment = Pos.CENTER_LEFT
                    title("Name")
                    // FIXME: Validate the name.
                    textfield(initialConfig.name) {
                        textProperty().onChange {
                            reportConfig = reportConfig.copy(name = it!!)
                        }
                    }
                }

                separator()

                vbox(spacing = 10) {
                    reportConfigProperty.perform { config ->
                        // TODO: This is probably leaking a lot of listeners.
                        replaceChildren {
                            title("Rules")
                            children += ReportRulesFragment(
                                modifier = { it::withRules },
                                reportConfigProperty = reportConfigProperty,
                                rootRule = config.rules
                            ).root

                            separator()

                            title("Filters")
                            children += ReportRulesFragment(
                                modifier = { it::withFilters },
                                reportConfigProperty = reportConfigProperty,
                                rootRule = config.filters
                            ).root
                        }
                    }
                }
            }
        }
    }

    private fun EventTarget.title(text: String) = label(text) { addClass(Style.ruleTitle )}

    fun show(): ReportConfig? {
        openWindow(block = true, owner = null)
        return if (accept) reportConfigProperty.value else null
    }

    private fun close(accept: Boolean) {
        this.accept = accept
        close()
    }

    init {
        reportConfigProperty.onChange { println(it) }
    }

    class Style : Stylesheet() {
        companion object {
            val ruleWindow by cssclass()
            val rulesContent by cssclass()
            val ruleTitle by cssclass()

            init {
                importStylesheet(Style::class)
            }
        }

        init {
            ruleWindow {
                minWidth = 800.px
                minHeight = 800.px
            }

            rulesContent {
                padding = box(20.px)
            }

            ruleTitle {
                fontSize = 16.px
                fontWeight = FontWeight.BOLD
            }
        }
    }
}