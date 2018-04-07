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

package com.gitlab.ykrasik.gamedex.ui.view.report

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.core.FilterSet
import com.gitlab.ykrasik.gamedex.core.api.provider.GameProviderRepository
import com.gitlab.ykrasik.gamedex.core.api.util.behaviorSubject
import com.gitlab.ykrasik.gamedex.core.api.util.modifyValue
import com.gitlab.ykrasik.gamedex.core.api.util.value_
import com.gitlab.ykrasik.gamedex.core.game.GameSettings
import com.gitlab.ykrasik.gamedex.javafx.*
import com.gitlab.ykrasik.gamedex.javafx.game.GameController
import com.gitlab.ykrasik.gamedex.javafx.library.LibraryController
import com.gitlab.ykrasik.gamedex.settings.ReportSettings
import com.gitlab.ykrasik.gamedex.ui.view.game.filter.FilterFragment
import javafx.beans.binding.Bindings
import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.StringProperty
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
class ReportConfigView : View("Report Config") {
    private val settings: ReportSettings by di()
    private val gameSettings: GameSettings by di()
    private val libraryController: LibraryController by di()
    private val gameController: GameController by di()
    private val providerRepository: GameProviderRepository by di()

    private val initialReportConfigSubject = behaviorSubject<ReportConfig>()
    private val currentReportConfigSubject = behaviorSubject<ReportConfig>().apply {
        initialReportConfigSubject.subscribe {
            this@apply.value_ = it
        }
    }

    // TODO: Move this logic to presenter.
    private val disallowedNames = initialReportConfigSubject.map { settings.reports.keys - it.name }.toObservableList()
    private val excludedGames = currentReportConfigSubject.map { it.excludedGames.map { gameController.byId(it) } }.toObservableList()

    private val reportNameProperty = SimpleStringProperty()
    private val viewModel = ReportNameViewModel(reportNameProperty)

    private val filterSet = FilterSet.Builder(gameSettings, libraryController, gameController, providerRepository).build()
    private val filterFragment = FilterFragment(currentReportConfigSubject.map { it.filter }, filterSet)

    private val isValid = viewModel.valid.and(filterFragment.isValid)

    private var accept = false

    init {
        initialReportConfigSubject.subscribe {
            reportNameProperty.value = it.name
        }
        filterFragment.newFilterObservable.subscribe { newFilter ->
            currentReportConfigSubject.modifyValue { it.copy(filter = newFilter) }
        }
    }

    // TODO: Generate name according to rules.toString unless custom name typed
    override val root = borderpane {
        minWidth = Region.USE_COMPUTED_SIZE
        minHeight = screenBounds.height * 1 / 2
        top {
            toolbar {
                acceptButton {
                    enableWhen { isValid }
                    setOnAction {
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
                            when {
                                it.isNullOrEmpty() -> error("Report name cannot be empty!")
                                disallowedNames.contains(it) -> error("Report with such a name already exists!")
                                else -> null
                            }
                        }
                    }
                }

                separator()

                hbox {
                    vbox(spacing = 10.0) {
                        header("Rules")
                        children += filterFragment.root
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
            makeIndexColumn().apply { addClass(CommonStyle.centered) }
            readonlyColumn("Game", Game::name)
            readonlyColumn("Path", Game::path)

            popoverContextMenu {
                deleteButton("Un-exclude") {
                    setOnAction {
                        currentReportConfigSubject.modifyValue {
                            it.copy(excludedGames = it.excludedGames - selectedItem!!.id)
                        }
                    }
                }
            }

            resizeColumnsToFitContent()
        }

    fun show(reportConfig: ReportConfig): ReportConfig? {
        initialReportConfigSubject.onNext(reportConfig)
        openWindow(block = true)
        return if (accept) currentReportConfigSubject.value_ else null
    }

    private fun close(accept: Boolean) {
        this.accept = accept
        close()
    }

    private inner class ReportNameViewModel(nameProperty: StringProperty) : ViewModel() {
        val textProperty = bind { nameProperty }

        init {
            textProperty.onChange { name ->
                if (commit()) {
                    currentReportConfigSubject.modifyValue { it.copy(name = name!!) }
                }
            }
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