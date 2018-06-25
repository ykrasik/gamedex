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

package com.gitlab.ykrasik.gamedex.core.report

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.app.api.ViewManager
import com.gitlab.ykrasik.gamedex.app.api.filter.Filter
import com.gitlab.ykrasik.gamedex.app.api.report.EditReportView
import com.gitlab.ykrasik.gamedex.app.api.report.ReportConfig
import com.gitlab.ykrasik.gamedex.core.Presentation
import com.gitlab.ykrasik.gamedex.core.Presenter
import com.gitlab.ykrasik.gamedex.core.api.game.GameService
import com.gitlab.ykrasik.gamedex.core.settings.SettingsService
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 24/06/2018
 * Time: 10:33
 */
@Singleton
class EditReportPresenter @Inject constructor(
    private val settingsService: SettingsService,
    private val gameService: GameService,
    private val viewManager: ViewManager
) : Presenter<EditReportView> {
    override fun present(view: EditReportView) = object : Presentation() {
        init {
            view.nameChanges.forEach { onNameChanged() }
            view.filterChanges.forEach { onFilterChanged() }

            view.unexcludeGameActions.forEach { onUnexcludeGame(it) }
            view.acceptActions.forEach { onAccept() }
            view.cancelActions.forEach { onCancel() }
        }

        override fun onShow() {
            val reportConfig = view.reportConfig
            view.name = reportConfig?.name ?: ""
            view.filter = reportConfig?.filter ?: Filter.`true`
            view.excludedGames.clear()
            view.excludedGames += reportConfig?.excludedGames?.map { gameService[it] } ?: emptyList()
            validateName()
        }

        private fun onNameChanged() {
            validateName()
        }

        private fun onFilterChanged() {
        }

        private fun validateName() {
            view.nameValidationError = when {
                view.name.isEmpty() -> "Name is required!"
                nameAlreadyUsed -> "Name already in use!"
                else -> null
            }
        }

        private val nameAlreadyUsed get() = view.name in (settingsService.report.reports.keys - view.reportConfig?.name)

        private fun onUnexcludeGame(game: Game) {
            view.excludedGames -= game
        }

        private fun onAccept() {
            check(view.nameValidationError == null) { "Cannot accept invalid state!" }
            val newReportConfig = ReportConfig(
                name = view.name,
                filter = view.filter,
                excludedGames = view.excludedGames.map { it.id }
            )
            settingsService.report.modify {
                modifyReports { reports ->
                    if (view.reportConfig != null) {
                        reports - view.reportConfig!!.name + (newReportConfig.name to newReportConfig)
                    } else {
                        reports + (newReportConfig.name to newReportConfig)
                    }
                }
            }
            close()
        }

        private fun onCancel() {
            close()
        }

        private fun close() {
            viewManager.closeEditReportView(view)
        }
    }
}