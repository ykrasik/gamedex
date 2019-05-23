/****************************************************************************
 * Copyright (C) 2016-2019 Yevgeny Krasik                                   *
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

package com.gitlab.ykrasik.gamedex.core.report.presenter

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.Timestamp
import com.gitlab.ykrasik.gamedex.app.api.filter.Filter
import com.gitlab.ykrasik.gamedex.app.api.filter.find
import com.gitlab.ykrasik.gamedex.app.api.filter.isEqual
import com.gitlab.ykrasik.gamedex.app.api.report.EditReportView
import com.gitlab.ykrasik.gamedex.app.api.report.ReportData
import com.gitlab.ykrasik.gamedex.core.EventBus
import com.gitlab.ykrasik.gamedex.core.Presenter
import com.gitlab.ykrasik.gamedex.core.ViewSession
import com.gitlab.ykrasik.gamedex.core.game.GameService
import com.gitlab.ykrasik.gamedex.core.report.ReportService
import com.gitlab.ykrasik.gamedex.core.task.TaskService
import com.gitlab.ykrasik.gamedex.util.Try
import com.gitlab.ykrasik.gamedex.util.and
import com.gitlab.ykrasik.gamedex.util.setAll
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 24/06/2018
 * Time: 10:33
 */
@Singleton
class EditReportPresenter @Inject constructor(
    private val reportService: ReportService,
    private val gameService: GameService,
    private val taskService: TaskService,
    private val eventBus: EventBus
) : Presenter<EditReportView> {
    override fun present(view: EditReportView) = object : ViewSession() {
        init {
            view.name.forEach { onNameChanged() }
            view.isTag.forEach { onIsTagChanged() }
            view.filter.forEach { onFilterChanged() }
            view.filterIsValid.forEach { setCanAccept() }

            view.unexcludeGameActions.forEach { onUnexcludeGame(it) }
            view.acceptActions.forEach { onAccept() }
            view.cancelActions.forEach { onCancel() }
        }

        override suspend fun onShown() {
            val report = view.report
            view.name *= report?.name ?: ""
            view.filter *= report?.filter ?: Filter.Null
            view.isTag *= report?.data?.isTag ?: true
            view.excludedGames.setAll(report?.excludedGames?.map { gameService[it] } ?: emptyList())
            validateName()
        }

        private fun onNameChanged() {
            validateName()
        }

        private fun onFilterChanged() {
            setCanAccept()
        }

        private fun onIsTagChanged() {
            setCanAccept()
        }

        private fun validateName() {
            view.nameIsValid *= Try {
                val name = view.name.value
                if (name.isEmpty()) error("Name is required!")
                if (name in (reportService.reports.map { it.name } - view.report?.name)) error("Name already in use!")
            }
            setCanAccept()
        }

        private fun setCanAccept() {
            view.canAccept *= view.nameIsValid.and(view.filterIsValid).and(Try {
                if (view.name.value == view.report?.name &&
                    view.isTag.value == view.report?.data?.isTag &&
                    view.filter.value.isEqual(view.report?.filter) &&
                    view.excludedGames.map { it.id } == view.report?.excludedGames) {
                    error("Nothing changed!")
                }
                if (view.isTag.value && view.filter.value.find(Filter.ReportTag::class) != null) {
                    error("Reports that tag games may not depend on ReportTag filters!")
                }
            })
        }

        private fun onUnexcludeGame(game: Game) {
            view.excludedGames -= game
            setCanAccept()
        }

        private suspend fun onAccept() {
            view.canAccept.assert()
            val newReportData = ReportData(
                name = view.name.value,
                filter = view.filter.value,
                isTag = view.isTag.value,
                excludedGames = view.excludedGames.map { it.id },
                timestamp = Timestamp.now
            )
            taskService.execute(
                if (view.report != null) {
                    reportService.update(view.report!!, newReportData)
                } else {
                    reportService.add(newReportData)
                }
            )
            hideView()
        }

        private fun onCancel() {
            hideView()
        }

        private fun hideView() = eventBus.requestHideView(view)
    }
}