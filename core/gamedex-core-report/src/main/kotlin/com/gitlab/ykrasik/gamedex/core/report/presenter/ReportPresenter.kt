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

package com.gitlab.ykrasik.gamedex.core.report.presenter

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.app.api.report.Report
import com.gitlab.ykrasik.gamedex.app.api.report.ReportResult
import com.gitlab.ykrasik.gamedex.app.api.report.ReportView
import com.gitlab.ykrasik.gamedex.core.Presenter
import com.gitlab.ykrasik.gamedex.core.ViewSession
import com.gitlab.ykrasik.gamedex.core.file.FileSystemService
import com.gitlab.ykrasik.gamedex.core.filter.FilterContextFactory
import com.gitlab.ykrasik.gamedex.core.game.GameService
import com.gitlab.ykrasik.gamedex.util.flatMapIndexed
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.withContext
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
    private val fileSystemService: FileSystemService,
    private val filterContextFactory: FilterContextFactory
) : Presenter<ReportView> {
    override fun present(view: ReportView) = object : ViewSession() {
        private var subscription: ReceiveChannel<*>? = null

        init {
            view.reportChanges.forEach { report ->
                subscription?.cancel()
                if (showing) {
                    subscription = gameService.games.itemsChannel.subscribe(Dispatchers.Main) { games ->
                        view.calculatingReport = true
                        view.result = report?.let { report ->
                            calculate(games, report)
                        }
                        view.calculatingReport = false
                    }
                }
            }
        }

        private suspend fun calculate(games: List<Game>, report: Report): ReportResult = withContext(Dispatchers.Default) {
            withContext(Dispatchers.Main) {
                view.calculatingReportProgress = 0.0
            }
            val context = filterContextFactory.create(games)
            // Report progress every 'chunkSize' games.
            val chunkSize = 50
            val matchingGames = games.chunked(chunkSize).flatMapIndexed { i, chunk ->
                val result = chunk.filter { game ->
                    !report.excludedGames.contains(game.id) && report.filter.evaluate(game, context)
                }
                withContext(Dispatchers.Main) {
                    view.calculatingReportProgress = (i * chunkSize + chunk.size.toDouble()) / games.size
                }
                result
            }
            ReportResult(
                games = matchingGames.sortedBy { it.name },
                additionalData = context.additionalData,
                fileStructure = fileSystemService.allStructure()
            )
        }

        override fun onHide() {
            subscription?.cancel()
            subscription = null
        }
    }
}