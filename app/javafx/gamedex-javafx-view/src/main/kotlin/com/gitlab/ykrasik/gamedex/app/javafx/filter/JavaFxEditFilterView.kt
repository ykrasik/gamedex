/****************************************************************************
 * Copyright (C) 2016-2020 Yevgeny Krasik                                   *
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

package com.gitlab.ykrasik.gamedex.app.javafx.filter

import com.gitlab.ykrasik.gamedex.app.api.filter.EditFilterView
import com.gitlab.ykrasik.gamedex.app.api.filter.Filter
import com.gitlab.ykrasik.gamedex.app.api.filter.NamedFilter
import com.gitlab.ykrasik.gamedex.app.api.util.ValidatedValue
import com.gitlab.ykrasik.gamedex.app.api.util.fromView
import com.gitlab.ykrasik.gamedex.app.api.util.writeFrom
import com.gitlab.ykrasik.gamedex.app.api.util.writeTo
import com.gitlab.ykrasik.gamedex.app.javafx.JavaFxViewManager
import com.gitlab.ykrasik.gamedex.javafx.addComponent
import com.gitlab.ykrasik.gamedex.javafx.control.*
import com.gitlab.ykrasik.gamedex.javafx.mutableStateFlow
import com.gitlab.ykrasik.gamedex.javafx.screenBounds
import com.gitlab.ykrasik.gamedex.javafx.theme.Icons
import com.gitlab.ykrasik.gamedex.javafx.theme.header
import com.gitlab.ykrasik.gamedex.javafx.view.ConfirmationWindow
import com.gitlab.ykrasik.gamedex.javafx.viewMutableStateFlow
import com.gitlab.ykrasik.gamedex.util.IsValid
import tornadofx.*

/**
 * User: ykrasik
 * Date: 17/06/2017
 * Time: 16:56
 */
class JavaFxEditFilterView : ConfirmationWindow(), EditFilterView {
    private val filterView = JavaFxFilterView(allowSaveLoad = false)

    private val viewManager: JavaFxViewManager by inject()
    private val overwriteFilterView = JavaFxFilterView(allowSaveLoad = false, readOnly = true)

    override val initialNamedFilter = viewMutableStateFlow(NamedFilter.Null, debugName = "initialNamedFilter")

    override val name = viewMutableStateFlow("", debugName = "name")
    override val nameIsValid = mutableStateFlow(IsValid.valid, debugName = "nameIsValid")

    override val filter = viewMutableStateFlow(Filter.Null, debugName = "filter")
        .writeTo(filterView.filter) { it.asFromView() }
        .writeFrom(filterView.filter) { it.asFromView() }

    override val filterValidatedValue = viewMutableStateFlow(ValidatedValue(Filter.Null, IsValid.valid), debugName = "filterValidatedValue")
        .writeFrom(filterView.filterValidatedValue) { it.fromView }

    override val isTag = viewMutableStateFlow(false, debugName = "isTag")

//    override val excludedGames = mutableListOf<Game>().asObservable()

//    override val unexcludeGameActions = broadcastFlow<Game>()

    init {
        titleProperty.bind(initialNamedFilter.property.stringBinding { if (it!!.isAnonymous) "Save Filter" else "Edit Filter" })
//        iconProperty.bind(reportProperty.objectBinding { if (it == null) Icons.add else Icons.edit })
        register()
    }

    override val root = borderpane {
        top = confirmationToolbar()
        center = prettyScrollPane {
            maxHeight = screenBounds.height / 2
            isFitToWidth = true
            isFitToHeight = true
            vbox(spacing = 10) {
                paddingAll = 10
                defaultHbox(spacing = 10) {
                    header("Name")

                    jfxTextField(name.property, promptText = "Enter Name...") {
                        validWhen(nameIsValid)
                    }

                    spacer()

                    jfxCheckBox(isTag.property, "Tag matching games")
                }

                verticalGap()

                addComponent(filterView)

//            defaultHbox {
//                vbox(spacing = 10) {
//                    header("Filters")
//                    addComponent(filterView)
//                }
//                gap { removeWhen { Bindings.isEmpty(excludedGames) } }
//                vbox(spacing = 10) {
//                    removeWhen { Bindings.isEmpty(excludedGames) }
//                    hgrow = Priority.ALWAYS
//                    header("Excluded Games")
//                    renderExcludedGames()
//                }
//            }
            }
        }
    }

    override suspend fun confirmOverwrite(filterToOverwrite: NamedFilter): Boolean {
        overwriteFilterView.filter *= filterToOverwrite.filter

        return viewManager.showAreYouSureDialog("Overwrite filter '${filterToOverwrite.id}'?", Icons.warning) {
            prettyScrollPane {
                maxHeight = screenBounds.height / 2
                isFitToWidth = true
                isFitToHeight = true
                content = overwriteFilterView.root
            }
        }
    }

//    private fun EventTarget.renderExcludedGames() = tableview(excludedGames) {
//        minWidth = 500.0
//        allowDeselection(onClickAgain = true)
//
//        makeIndexColumn().apply { addClass(GameDexStyle.centered) }
//        readonlyColumn("Game", Game::name).apply { addClass(GameDexStyle.centered) }
//        readonlyColumn("Path", Game::path) { addClass(GameDexStyle.centered); remainingWidth() }
//        customGraphicColumn("") { game ->
//            minusButton { action(unexcludeGameActions) { game } }
//        }
//
//        excludedGames.perform { resizeColumnsToFitContent() }
//    }
}