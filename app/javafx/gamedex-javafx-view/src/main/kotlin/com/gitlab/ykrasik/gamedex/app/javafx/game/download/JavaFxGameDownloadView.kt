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

package com.gitlab.ykrasik.gamedex.app.javafx.game.download

import com.gitlab.ykrasik.gamedex.app.api.game.CreatedBeforePeriodView
import com.gitlab.ykrasik.gamedex.app.api.game.UpdatedAfterPeriodView
import com.gitlab.ykrasik.gamedex.app.api.game.ViewCanRedownloadGamesCreatedBefore
import com.gitlab.ykrasik.gamedex.app.api.game.ViewCanRedownloadGamesUpdatedAfter
import com.gitlab.ykrasik.gamedex.app.api.util.channel
import com.gitlab.ykrasik.gamedex.javafx.Theme
import com.gitlab.ykrasik.gamedex.javafx.buttonWithPopover
import com.gitlab.ykrasik.gamedex.javafx.redownloadButton
import com.gitlab.ykrasik.gamedex.javafx.view.PresentableView
import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Pos
import javafx.scene.layout.GridPane
import kotlinx.coroutines.experimental.channels.Channel
import org.controlsfx.control.PopOver
import tornadofx.*

/**
 * User: ykrasik
 * Date: 05/06/2017
 * Time: 10:57
 */
class JavaFxGameDownloadView : PresentableView(),
    CreatedBeforePeriodView, UpdatedAfterPeriodView, ViewCanRedownloadGamesCreatedBefore, ViewCanRedownloadGamesUpdatedAfter {
    
    override val createdBeforePeriodTextChanges = channel<String>()
    private val createdBeforePeriodViewModel = PeriodViewModel(createdBeforePeriodTextChanges)
    override var createdBeforePeriodText by createdBeforePeriodViewModel.periodTextProperty
    override var createdBeforePeriodValidationError by createdBeforePeriodViewModel.errorProperty
    
    override val updatedAfterPeriodTextChanges = channel<String>()
    private val updatedAfterPeriodViewModel = PeriodViewModel(updatedAfterPeriodTextChanges)
    override var updatedAfterPeriodText by updatedAfterPeriodViewModel.periodTextProperty
    override var updatedAfterPeriodValidationError by updatedAfterPeriodViewModel.errorProperty

    private inner class PeriodViewModel(changes: Channel<String>) : ViewModel() {
        val periodTextProperty = presentableStringProperty(changes)
        val errorProperty = SimpleStringProperty(null)
    }
    
    override val redownloadGamesCreatedBeforeActions = channel<Unit>()
    override val redownloadGamesUpdatedAfterActions = channel<Unit>()

    init {
        viewRegistry.onCreate(this)
    }

    override val root = buttonWithPopover("Re-Download", graphic = Theme.Icon.download(), arrowLocation = PopOver.ArrowLocation.TOP_RIGHT, closeOnClick = false) { popover ->
        gridpane {
            hgap = 5.0
            vgap = 5.0
            periodButton("Games Created Before", createdBeforePeriodViewModel, redownloadGamesCreatedBeforeActions, popover)
            periodButton("Games Updated After", updatedAfterPeriodViewModel, redownloadGamesUpdatedAfterActions, popover)
        }
    }

    private fun GridPane.periodButton(label: String, viewModel: PeriodViewModel, actions: Channel<Unit>, popOver: PopOver) {
        row {
            redownloadButton(label) {
                useMaxWidth = true
                alignment = Pos.CENTER_LEFT
                disableWhen(viewModel.valid.not())
                eventOnAction(actions) { popOver.hide() }
            }
            textfield(viewModel.periodTextProperty) {
                isFocusTraversable = false
                validatorFrom(viewModel, viewModel.errorProperty)
            }
        }
    }
}