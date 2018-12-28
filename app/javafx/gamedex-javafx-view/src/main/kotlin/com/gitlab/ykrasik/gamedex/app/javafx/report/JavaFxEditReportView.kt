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
import com.gitlab.ykrasik.gamedex.app.api.util.channel
import com.gitlab.ykrasik.gamedex.app.javafx.filter.JavaFxGameFilterView
import com.gitlab.ykrasik.gamedex.javafx.*
import com.gitlab.ykrasik.gamedex.javafx.control.*
import com.gitlab.ykrasik.gamedex.javafx.view.ConfirmationWindow
import com.gitlab.ykrasik.gamedex.util.IsValid
import javafx.beans.binding.Bindings
import javafx.beans.property.SimpleObjectProperty
import javafx.event.EventTarget
import javafx.scene.layout.Priority
import tornadofx.*

/**
 * User: ykrasik
 * Date: 17/06/2017
 * Time: 16:56
 */
class JavaFxEditReportView : ConfirmationWindow(), EditReportView {
    private val filterView = JavaFxGameFilterView(onlyShowConditionsForCurrentPlatform = false)

    private val reportProperty = SimpleObjectProperty<Report?>(null)
    override var report by reportProperty

    override val name = userMutableState("")
    override var nameIsValid = state(IsValid.valid)

    override val filter = userMutableState(filterView.filter)
    override val filterIsValid = userMutableState(filterView.filterIsValid)

    override val excludedGames = mutableListOf<Game>().observable()

    override val unexcludeGameActions = channel<Game>()

    init {
        titleProperty.bind(reportProperty.stringBinding { if (it == null) "Add New Report" else "Edit Report '${it.name}'" })
        excludedGames.onChange { resizeToContent() }
        filter.onChange { resizeToContent() }
        register()
    }

    override val root = borderpane {
        top = confirmationToolbar()
        center = vbox(spacing = 10) {
            paddingAll = 10
            header(titleProperty)
            defaultHbox(spacing = 10) {
                header("Name")

                jfxTextField(name.property, promptText = "Report Name") {
                    validWhen(nameIsValid)
                }
            }

            verticalGap()

            defaultHbox {
                vbox(spacing = 10) {
                    header("Rules")
                    addComponent(filterView)
                }
                gap { removeWhen { Bindings.isEmpty(excludedGames) } }
                vbox(spacing = 10) {
                    removeWhen { Bindings.isEmpty(excludedGames) }
                    hgrow = Priority.ALWAYS
                    header("Excluded Games")
                    renderExcludedGames()
                }
            }
        }
    }

    private fun EventTarget.renderExcludedGames() = tableview(excludedGames) {
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
}