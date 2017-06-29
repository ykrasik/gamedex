package com.gitlab.ykrasik.gamedex.ui.view.report

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.controller.GameController
import com.gitlab.ykrasik.gamedex.core.ReportConfig
import com.gitlab.ykrasik.gamedex.settings.ReportSettings
import com.gitlab.ykrasik.gamedex.ui.map
import com.gitlab.ykrasik.gamedex.ui.perform
import com.gitlab.ykrasik.gamedex.ui.popoverContextMenu
import com.gitlab.ykrasik.gamedex.ui.theme.CommonStyle
import com.gitlab.ykrasik.gamedex.ui.theme.acceptButton
import com.gitlab.ykrasik.gamedex.ui.theme.cancelButton
import com.gitlab.ykrasik.gamedex.ui.theme.deleteButton
import com.gitlab.ykrasik.gamedex.ui.verticalSeparator
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.event.EventTarget
import javafx.geometry.Pos
import javafx.scene.layout.Pane
import javafx.scene.layout.Priority
import javafx.scene.text.FontWeight
import tornadofx.*

/**
 * User: ykrasik
 * Date: 17/06/2017
 * Time: 16:56
 */
class ReportConfigFragment(initialConfig: ReportConfig) : Fragment("Report Config") {
    private val settings: ReportSettings by di()
    private val gameController: GameController by di()

    private val reportConfigProperty = SimpleObjectProperty(initialConfig)
    private var reportConfig by reportConfigProperty

    private val unallowedNames = settings.reports.keys - initialConfig.name
    private val viewModel = ReportNameViewModel(initialConfig.name)

    private var accept = false

    // TODO: Generate name according to rules.toString unless custom name typed
    override val root = borderpane {
        if (reportConfig.excludedGames.isEmpty()) {
            minWidth = 800.0
        } else {
            minWidth = 1300.0
        }
        minHeight = 800.0
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

                hbox {
                    reportConfigProperty.perform { reportConfig ->
                        // TODO: This is probably leaking a lot of listeners.
                        replaceChildren {
                            vbox(spacing = 10.0) {
                                hgrow = Priority.ALWAYS
                                title("Rules")
                                renderRules()
                            }
                            if (reportConfig.excludedGames.isNotEmpty()) {
                                verticalSeparator()
                                vbox(spacing = 10.0) {
                                    hgrow = Priority.ALWAYS
                                    title("Excluded Games")
                                    renderExcludedGames()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun EventTarget.title(text: String) = label(text) { addClass(Style.ruleTitle) }

    private fun Pane.renderRules() {
        children += ReportRuleFragment(reportConfigProperty).root
    }

    private fun EventTarget.renderExcludedGames() =
        tableview(reportConfig.excludedGames.map { gameController.byId(it) }.observable()) {
            makeIndexColumn().apply { addClass(CommonStyle.centered) }
            column("Game", Game::name)
            column("Path", Game::path)

            popoverContextMenu {
                deleteButton("Un-exclude") {
                    setOnAction {
                        reportConfig = reportConfig.copy(excludedGames = (reportConfig.excludedGames - selectedItem!!.id))
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

    private class ReportNameViewModel(initialName: String) : ViewModel() {
        val textProperty = bind { SimpleStringProperty(initialName) }
        var text by textProperty

        init {
            textProperty.onChange { commit() }
            validate(decorateErrors = true)
        }
    }

    class Style : Stylesheet() {
        companion object {
            val rulesContent by cssclass()
            val ruleTitle by cssclass()
            val ruleButton by cssclass()

            init {
                importStylesheet(Style::class)
            }
        }

        init {
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