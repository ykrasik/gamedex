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

import com.gitlab.ykrasik.gamedex.app.api.report.Report
import com.gitlab.ykrasik.gamedex.app.api.report.ReportView
import com.gitlab.ykrasik.gamedex.app.api.util.MultiChannel
import com.gitlab.ykrasik.gamedex.core.EventBus
import com.gitlab.ykrasik.gamedex.core.Presenter
import com.gitlab.ykrasik.gamedex.core.ViewSession
import com.gitlab.ykrasik.gamedex.core.game.GameService
import com.gitlab.ykrasik.gamedex.core.report.ReportService
import com.gitlab.ykrasik.gamedex.core.task.TaskService
import com.gitlab.ykrasik.gamedex.core.util.ListEvent
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 29/06/2018
 * Time: 10:30
 */
@Singleton
class ReportPresenter @Inject constructor(
    private val gameService: GameService,
    private val reportService: ReportService,
    private val taskService: TaskService,
    private val eventBus: EventBus
) : Presenter<ReportView> {
    override fun present(view: ReportView) = object : ViewSession() {
        private val isReportDirtyChannel = MultiChannel.conflated(false)
        private var isReportDirty by isReportDirtyChannel

        init {
            view.report.forEach { isReportDirty = true }
            gameService.games.itemsChannel.forEach { isReportDirty = true }

            isReportDirtyChannel.forEach { isReportDirty ->
                val report = view.report.value
                if (isReportDirty && isShowing && report != Report.Null) {
                    view.result *= taskService.execute(reportService.calc(report, gameService.games))
                    this.isReportDirty = false
                }
            }

            reportService.reports.changesChannel.forEach { e ->
                val report = view.report.value
                if (report == Report.Null) return@forEach

                when (e) {
                    is ListEvent.ItemRemoved -> {
                        if (e.item == report) hideView()
                    }
                    is ListEvent.ItemsRemoved -> {
                        if (e.items.contains(report)) hideView()
                    }
                    is ListEvent.ItemSet -> {
                        if (e.item.id == report.id) {
                            changeReport(e.item)
                        }
                    }
                    is ListEvent.ItemsSet -> {
                        val relevantReport = e.items.find { it.id == report.id }
                        if (relevantReport != null) {
                            changeReport(relevantReport)
                        }
                    }
                    else -> {
                        // Ignored
                    }
                }
            }
        }

        private fun changeReport(report: Report) {
            if (view.report.value != report) {
                view.report *= report
                isReportDirty = true
            }
        }

        override suspend fun onShown() {
            // Send the existing 'reportDirty' value to the channel again, to cause the consumer to re-run
            isReportDirty = isReportDirty
        }

        private fun hideView() {
            view.report *= Report.Null
            eventBus.requestHideView(view)
        }
    }
}