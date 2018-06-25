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

package com.gitlab.ykrasik.gamedex.app.javafx.report

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.app.api.filter.Filter
import com.gitlab.ykrasik.gamedex.app.api.report.EditReportView
import com.gitlab.ykrasik.gamedex.app.api.report.ReportConfig
import com.gitlab.ykrasik.gamedex.app.api.util.channel
import com.gitlab.ykrasik.gamedex.app.javafx.filter.JavaFxReportGameFilterView
import com.gitlab.ykrasik.gamedex.javafx.*
import com.gitlab.ykrasik.gamedex.javafx.view.PresentableView
import javafx.beans.binding.Bindings
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.event.EventTarget
import javafx.geometry.Pos
import javafx.scene.layout.Priority
import javafx.scene.layout.Region
import tornadofx.*

/**
 * User: ykrasik
 * Date: 17/06/2017
 * Time: 16:56
 */
class JavaFxEditReportView : PresentableView(), EditReportView {
    private val filterView: JavaFxReportGameFilterView by inject()

    private val reportConfigProperty = SimpleObjectProperty<ReportConfig?>(null)
    override var reportConfig by reportConfigProperty

    override val nameChanges = channel<String>()
    override val filterChanges = channel<Filter>()

    private val viewModel = ReportViewModel()
    override var name by viewModel.nameProperty
    override var filter by filterView.filterProperty

    private inner class ReportViewModel : ViewModel() {
        val nameProperty = presentableStringProperty(nameChanges)
    }

    private val nameValidationErrorProperty = SimpleStringProperty(null)
    override var nameValidationError by nameValidationErrorProperty

    override val excludedGames = mutableListOf<Game>().observable()

    override val unexcludeGameActions = channel<Game>()
    override val acceptActions = channel<Unit>()
    override val cancelActions = channel<Unit>()

    private val isValid = viewModel.valid.and(filterView.isValid)

    init {
        titleProperty.bind(reportConfigProperty.stringBinding { if (it == null) "Add New Report" else "Edit Report '${it.name}'" })
        excludedGames.onChange { currentStage?.sizeToScene() }
        filterView.filterProperty.onChange { currentStage?.sizeToScene() }
        viewRegistry.register(this)
    }

    override val root = borderpane {
        minWidth = Region.USE_COMPUTED_SIZE
        minHeight = screenBounds.height * 1 / 2
        top {
            toolbar {
                acceptButton {
                    enableWhen { isValid }
                    eventOnAction(acceptActions)
                }
                verticalSeparator()
                spacer()
                verticalSeparator()
                cancelButton { eventOnAction(cancelActions) }
            }
        }
        center {
            vbox(spacing = 10) {
                addClass(Style.rulesContent)
                hbox(spacing = 10) {
                    alignment = Pos.CENTER_LEFT
                    header("Name")

                    textfield(viewModel.nameProperty) {
                        validatorFrom(viewModel, nameValidationErrorProperty)
                    }
                }

                separator()

                hbox {
                    vbox(spacing = 10.0) {
                        header("Rules")
                        children += filterView.root
                    }
                    verticalSeparator { removeWhen { Bindings.isEmpty(excludedGames) } }
                    vbox(spacing = 10.0) {
                        removeWhen { Bindings.isEmpty(excludedGames) }
                        hgrow = Priority.ALWAYS
                        header("Excluded Games")
                        renderExcludedGames()
                    }
                }
            }
        }
    }

    private fun EventTarget.renderExcludedGames() =
        tableview(excludedGames) {
            minWidth = 500.0
            allowDeselection(onClickAgain = true)

            makeIndexColumn().apply { addClass(CommonStyle.centered) }
            readonlyColumn("Game", Game::name)
            readonlyColumn("Path", Game::path) { remainingWidth() }
            customGraphicColumn("") { game ->
                jfxButton(graphic = Theme.Icon.minus(12.0)) { eventOnAction(unexcludeGameActions) { game } }
            }

            excludedGames.perform { resizeColumnsToFitContent() }
        }

    class Style : Stylesheet() {
        companion object {
            val rulesContent by cssclass()

            init {
                importStylesheetSafe(Style::class)
            }
        }

        init {
            rulesContent {
                padding = box(20.px)
            }
        }
    }
}