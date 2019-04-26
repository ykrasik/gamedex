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

package com.gitlab.ykrasik.gamedex.core.report

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.app.api.filter.Filter
import com.gitlab.ykrasik.gamedex.app.api.report.Report
import com.gitlab.ykrasik.gamedex.app.api.report.ReportData
import com.gitlab.ykrasik.gamedex.app.api.report.ReportId
import com.gitlab.ykrasik.gamedex.app.api.report.ReportResult
import com.gitlab.ykrasik.gamedex.core.filter.FilterContextFactory
import com.gitlab.ykrasik.gamedex.core.task.task
import com.gitlab.ykrasik.gamedex.util.flatMapIndexed
import com.gitlab.ykrasik.gamedex.util.logger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 19/09/2018
 * Time: 19:24
 */
@Singleton
class ReportServiceImpl @Inject constructor(
    private val repo: ReportRepository,
    private val filterContextFactory: FilterContextFactory
) : ReportService {
    private val log = logger()

    override val reports = repo.reports

    init {
        if (reports.isEmpty()) {
            log.info("Creating default reports...")
            defaultReports.forEach { repo.add(it) }
        }
    }

    override fun get(id: ReportId) = reports.find { it.id == id } ?: throw IllegalArgumentException("Report($id) doesn't exist!")

    override fun add(data: ReportData) = task("Adding Report '${data.name}'...") {
        successMessage = { "Added Report: '${data.name}'." }
        repo.add(data)
    }

    override fun update(report: Report, data: ReportData) = task("Updating Report '${report.name}'...") {
        val updatedReport = repo.update(report, data)
        successMessage = { "Updated Report: '${updatedReport.name}'." }
        updatedReport
    }

    override fun delete(report: Report) = task("Deleting Report '${report.name}'...") {
        successMessage = { "Deleted Report: '${report.name}'." }
        repo.delete(report)
    }

    override fun calc(report: Report, games: List<Game>) = task("Calculating report '${report.name}'...") {
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
    }

    fun invalidate() = repo.invalidate()

    private companion object {
        val defaultReports = listOf(
            ReportData("Name Diff", Filter.NameDiff()),
            ReportData("Low Score", Filter.CriticScore(60.0).not or Filter.UserScore(60.0).not),
            ReportData("Very Low Score", Filter.CriticScore(60.0).not and Filter.UserScore(60.0).not),
            ReportData("No Score", Filter.CriticScore(0.0).not and Filter.UserScore(0.0).not)
        )
    }
}