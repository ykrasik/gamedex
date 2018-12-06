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
import com.gitlab.ykrasik.gamedex.app.api.report.EditReportView
import com.gitlab.ykrasik.gamedex.app.api.report.Report
import com.gitlab.ykrasik.gamedex.app.api.util.IsValid
import com.gitlab.ykrasik.gamedex.app.api.util.channel
import com.gitlab.ykrasik.gamedex.app.javafx.filter.JavaFxGameFilterView
import com.gitlab.ykrasik.gamedex.javafx.*
import com.gitlab.ykrasik.gamedex.javafx.control.*
import com.gitlab.ykrasik.gamedex.javafx.view.PresentableView
import javafx.beans.binding.Bindings
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.event.EventTarget
import javafx.scene.layout.Priority
import javafx.scene.layout.Region
import tornadofx.*

/**
 * User: ykrasik
 * Date: 17/06/2017
 * Time: 16:56
 */
class JavaFxEditReportView : PresentableView(), EditReportView {
    private val filterView = JavaFxGameFilterView(onlyShowConditionsForCurrentPlatform = false)

    private val reportProperty = SimpleObjectProperty<Report?>(null)
    override var report by reportProperty

    private val canAcceptProperty = SimpleObjectProperty(IsValid.valid)
    override var canAccept by canAcceptProperty

    override val nameChanges = channel<String>()
    private val nameProperty = SimpleStringProperty("").eventOnChange(nameChanges)
    override var name by nameProperty

    private val nameIsValidProperty = SimpleObjectProperty(IsValid.valid)
    override var nameIsValid by nameIsValidProperty

    override var filter by filterView.filterProperty
    override val filterChanges = filterView.filterChanges

    override val excludedGames = mutableListOf<Game>().observable()

    override val unexcludeGameActions = channel<Game>()
    override val acceptActions = channel<Unit>()
    override val cancelActions = channel<Unit>()

    init {
        titleProperty.bind(reportProperty.stringBinding { if (it == null) "Add New Report" else "Edit Report '${it.name}'" })
        excludedGames.onChange { currentStage?.sizeToScene() }
        filterView.filterProperty.onChange { currentStage?.sizeToScene() }
        viewRegistry.onCreate(this)
    }

    override val root = borderpane {
        minWidth = Region.USE_COMPUTED_SIZE
        minHeight = screenBounds.height * 1 / 2
        top {
            toolbar {
                cancelButton { eventOnAction(cancelActions) }
                spacer()
                acceptButton {
                    enableWhen(canAcceptProperty.and(filterView.isValid))
                    eventOnAction(acceptActions)
                }
            }
        }
        center {
            vbox(spacing = 10) {
                addClass(Style.rulesContent)
                defaultHbox(spacing = 10) {
                    header("Name")

                    jfxTextField(nameProperty, promptText = "Report Name") {
                        validWhen(nameIsValidProperty)
                    }
                }

                verticalGap()

                defaultHbox {
                    vbox(spacing = 10.0) {
                        header("Rules")
                        addComponent(filterView)
                    }
                    gap { removeWhen { Bindings.isEmpty(excludedGames) } }
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
            readonlyColumn("Game", Game::name).apply { addClass(CommonStyle.centered) }
            readonlyColumn("Path", Game::path) { addClass(CommonStyle.centered); remainingWidth() }
            customGraphicColumn("") { game ->
                minusButton { eventOnAction(unexcludeGameActions) { game } }
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