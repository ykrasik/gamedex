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
import com.gitlab.ykrasik.gamedex.app.api.report.Report
import com.gitlab.ykrasik.gamedex.app.api.report.ReportResult
import com.gitlab.ykrasik.gamedex.app.api.report.ReportView
import com.gitlab.ykrasik.gamedex.app.api.util.BroadcastEventChannel
import com.gitlab.ykrasik.gamedex.core.EventBus
import com.gitlab.ykrasik.gamedex.core.Presenter
import com.gitlab.ykrasik.gamedex.core.ViewSession
import com.gitlab.ykrasik.gamedex.core.filter.FilterContextFactory
import com.gitlab.ykrasik.gamedex.core.game.GameService
import com.gitlab.ykrasik.gamedex.core.report.ReportService
import com.gitlab.ykrasik.gamedex.core.task.TaskService
import com.gitlab.ykrasik.gamedex.core.task.task
import com.gitlab.ykrasik.gamedex.core.util.ListEvent
import com.gitlab.ykrasik.gamedex.util.flatMapIndexed
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
    private val filterContextFactory: FilterContextFactory,
    private val taskService: TaskService,
    private val eventBus: EventBus
) : Presenter<ReportView> {
    override fun present(view: ReportView) = object : ViewSession() {
        private val isReportDirtyChannel = BroadcastEventChannel.conflated(false)
        private var isReportDirty by isReportDirtyChannel

        init {
            view.report.forEach { isReportDirty = true }
            gameService.games.itemsChannel.forEach { isReportDirty = true }

            isReportDirtyChannel.forEach { isReportDirty ->
                val report = view.report.value
                if (isReportDirty && isShowing && report != Report.Null) {
                    calculate(gameService.games, report)
                    this.isReportDirty = false
                }
            }

            reportService.reports.changesChannel.forEach { e ->
                val report = view.report.value
                if (report == Report.Null) return@forEach

                when (e) {
                    is ListEvent.ItemRemoved -> {
                        if (e.item == report) finished()
                    }
                    is ListEvent.ItemsRemoved -> {
                        if (e.items.contains(report)) finished()
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

        override suspend fun onShow() {
            // Send the existing 'reportDirty' value to the channel again, to cause the consumer to re-run
            isReportDirty = isReportDirty
        }

        private suspend fun calculate(games: List<Game>, report: Report) {
            view.result *= taskService.execute(task("Calculating report '${report.name}'...") {
                val context = filterContextFactory.create(games)

                totalItems = games.size
                // Report progress every 'chunkSize' games.
                val chunkSize = 50
                val matchingGames = games.chunked(chunkSize).flatMapIndexed { i, chunk ->
                    val result = chunk.filter { game ->
                        !report.excludedGames.contains(game.id) && report.filter.evaluate(game, context)
                    }
                    processedItems = i * chunkSize + chunk.size
                    result
                }
                ReportResult(
                    games = matchingGames.sortedBy { it.name },
                    additionalData = context.additionalData
                )
            })
        }

        private fun finished() {
            view.report *= Report.Null
            eventBus.viewFinished(view)
        }
    }
}