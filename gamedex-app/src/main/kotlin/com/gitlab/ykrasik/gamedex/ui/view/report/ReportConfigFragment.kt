package com.gitlab.ykrasik.gamedex.ui.view.report

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.controller.GameController
import com.gitlab.ykrasik.gamedex.controller.LibraryController
import com.gitlab.ykrasik.gamedex.core.FilterSet
import com.gitlab.ykrasik.gamedex.repository.GameProviderRepository
import com.gitlab.ykrasik.gamedex.settings.GameSettings
import com.gitlab.ykrasik.gamedex.settings.ReportSettings
import com.gitlab.ykrasik.gamedex.ui.perform
import com.gitlab.ykrasik.gamedex.ui.popoverContextMenu
import com.gitlab.ykrasik.gamedex.ui.theme.*
import com.gitlab.ykrasik.gamedex.ui.verticalSeparator
import com.gitlab.ykrasik.gamedex.ui.view.game.filter.FilterFragment
import com.jfoenix.controls.JFXButton
import javafx.beans.property.SimpleStringProperty
import javafx.event.EventTarget
import javafx.geometry.Pos
import javafx.scene.layout.Pane
import javafx.scene.layout.Priority
import tornadofx.*

/**
 * User: ykrasik
 * Date: 17/06/2017
 * Time: 16:56
 */
class ReportConfigFragment(initialConfig: ReportConfig) : Fragment("Report Config") {
    private val settings: ReportSettings by di()
    private val gameSettings: GameSettings by di()
    private val libraryController: LibraryController by di()
    private val gameController: GameController by di()
    private val providerRepository: GameProviderRepository by di()

    private val filterSet = FilterSet.Builder(gameSettings, libraryController, gameController, providerRepository).build()

    private val reportConfigProperty = initialConfig.toProperty()
    private var reportConfig by reportConfigProperty

    private val unallowedNames = settings.reports.keys - initialConfig.name
    private val viewModel = ReportNameViewModel(initialConfig.name)

    private var acceptButton: JFXButton by singleAssign()

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
                acceptButton = acceptButton {
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
                    header("Name")

                    textfield(viewModel.textProperty) {
                        validator {
                            if (it.isNullOrEmpty()) error("Report name cannot be empty!")
                            else if (unallowedNames.contains(it)) error("Report with such a name already exists!")
                            else null
                        }
                    }
                }

                separator()

                hbox {
                    reportConfigProperty.perform { reportConfig ->
                        // TODO: This is probably leaking a lot of listeners.
                        replaceChildren {
                            vbox(spacing = 10.0) {
                                hgrow = Priority.ALWAYS
                                header("Rules")
                                renderRules()
                            }
                            if (reportConfig.excludedGames.isNotEmpty()) {
                                verticalSeparator()
                                vbox(spacing = 10.0) {
                                    hgrow = Priority.ALWAYS
                                    header("Excluded Games")
                                    renderExcludedGames()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun Pane.renderRules() {
        val filterProperty = reportConfig.filter.toProperty()
        val fragment = FilterFragment(filterProperty, filterSet)
        filterProperty.onChange {
            reportConfig = reportConfig.copy(filter = it!!)
        }
        acceptButton.enableWhen { viewModel.valid.and(fragment.isValid) }
        children += fragment.root
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

            resizeColumnsToFitContent()
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

            init {
                importStylesheet(Style::class)
            }
        }

        init {
            rulesContent {
                padding = box(20.px)
            }
        }
    }
}