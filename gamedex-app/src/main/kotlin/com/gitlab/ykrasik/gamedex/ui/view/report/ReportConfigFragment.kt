package com.gitlab.ykrasik.gamedex.ui.view.report

import com.gitlab.ykrasik.gamedex.core.ReportConfig
import com.gitlab.ykrasik.gamedex.core.ReportRule
import com.gitlab.ykrasik.gamedex.ui.perform
import com.gitlab.ykrasik.gamedex.ui.theme.acceptButton
import com.gitlab.ykrasik.gamedex.ui.theme.cancelButton
import com.gitlab.ykrasik.gamedex.ui.verticalSeparator
import javafx.beans.property.SimpleObjectProperty
import javafx.scene.text.FontWeight
import tornadofx.*

/**
 * User: ykrasik
 * Date: 17/06/2017
 * Time: 16:56
 */
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
            vbox {
                addClass(Style.rulesContent)
                // FIXME: Make something less ugly, and validate the name.
                textfield(initialConfig.name) {
                    textProperty().onChange {
                        reportConfig = reportConfig.copy(name = it!!)
                    }
                }
                vbox(spacing = 10) {
                    reportConfigProperty.perform { config ->
                        // TODO: This is probably leaking a lot of listeners.
                        replaceChildren {
                            label("Rules") { addClass(Style.ruleTitle) }
                            children += ReportRulesFragment(
                                all = ReportRule.Rules.all,
                                defaultValue = { ReportRule.Rules.True() },
                                modifier = { it::withRules },
                                reportConfigProperty = reportConfigProperty,
                                rule = config.rules
                            ).root

                            separator()

                            label("Filters") { addClass(Style.ruleTitle) }
                            children += ReportRulesFragment(
                                all = ReportRule.Filters.all,
                                defaultValue = { ReportRule.Filters.True() },
                                modifier = { it::withFilters },
                                reportConfigProperty = reportConfigProperty,
                                rule = config.filters
                            ).root
                        }
                    }
                }
            }
        }
    }

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