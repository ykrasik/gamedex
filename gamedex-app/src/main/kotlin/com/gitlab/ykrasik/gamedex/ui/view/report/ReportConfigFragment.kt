package com.gitlab.ykrasik.gamedex.ui.view.report

import com.gitlab.ykrasik.gamedex.core.ReportConfig
import com.gitlab.ykrasik.gamedex.settings.ReportSettings
import com.gitlab.ykrasik.gamedex.ui.map
import com.gitlab.ykrasik.gamedex.ui.perform
import com.gitlab.ykrasik.gamedex.ui.theme.acceptButton
import com.gitlab.ykrasik.gamedex.ui.theme.cancelButton
import com.gitlab.ykrasik.gamedex.ui.verticalSeparator
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.event.EventTarget
import javafx.geometry.Pos
import javafx.scene.text.FontWeight
import tornadofx.*

/**
 * User: ykrasik
 * Date: 17/06/2017
 * Time: 16:56
 */
class ReportConfigFragment(initialConfig: ReportConfig) : Fragment("Report Config") {
    private val settings: ReportSettings by di()

    private val reportConfigProperty = SimpleObjectProperty(initialConfig)
    private var reportConfig by reportConfigProperty

    private val unallowedNames = settings.reports.keys - initialConfig.name
    private val viewModel = ReportNameViewModel(initialConfig.name).apply {
        textProperty.onChange { commit() }
        validate(decorateErrors = false)
    }

    private var accept = false

    // TODO: Generate name according to rules.toString unless custom name typed
    override val root = borderpane {
        addClass(Style.ruleWindow)
        top {
            toolbar {
                acceptButton {
                    enableWhen { viewModel.valid }
                    setOnAction {
                        reportConfig = reportConfig.copy(name = viewModel.text)
                        close(accept = true)
                    }
                }
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

                    textfield(viewModel.textProperty) {
                        validator {
                            if (it.isNullOrEmpty()) error("Report name cannot be empty!")
                            else if (unallowedNames.contains(it)) error("Report with such a name already exists!")
                            else null
                        }
                    }
                }

                separator()

                label {
                    isWrapText = true
                    textProperty().bind(reportConfigProperty.map { it!!.rules.toString() })
                }

                separator()

                vbox(spacing = 10) {
                    reportConfigProperty.perform {
                        // TODO: This is probably leaking a lot of listeners.
                        replaceChildren {
                            title("Rules")
                            children += ReportRuleFragment(reportConfigProperty).root
                        }
                    }
                }
            }
        }
    }

    private fun EventTarget.title(text: String) = label(text) { addClass(Style.ruleTitle) }

    fun show(): ReportConfig? {
        openWindow(block = true, owner = null)
        return if (accept) reportConfigProperty.value else null
    }

    private fun close(accept: Boolean) {
        this.accept = accept
        close()
    }

    private class ReportNameViewModel(initialName: String) : ViewModel() {
        val textProperty = bind { SimpleStringProperty(initialName) }
        var text by textProperty
    }

    class Style : Stylesheet() {
        companion object {
            val ruleWindow by cssclass()
            val rulesContent by cssclass()
            val ruleTitle by cssclass()
            val ruleButton by cssclass()

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

            ruleButton {
                minWidth = 120.px
                alignment = Pos.CENTER_LEFT
            }
        }
    }
}