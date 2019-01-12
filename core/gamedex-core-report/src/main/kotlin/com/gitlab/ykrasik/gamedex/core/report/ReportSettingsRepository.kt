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

import com.gitlab.ykrasik.gamedex.app.api.report.Report
import com.gitlab.ykrasik.gamedex.app.api.report.ReportData
import com.gitlab.ykrasik.gamedex.app.api.report.ReportId
import com.gitlab.ykrasik.gamedex.app.api.util.ListObservableImpl
import com.gitlab.ykrasik.gamedex.core.storage.Storage
import com.gitlab.ykrasik.gamedex.util.logger
import com.gitlab.ykrasik.gamedex.util.time
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 24/06/2018
 * Time: 16:22
 */
@Singleton
class ReportSettingsRepository @Inject constructor(private val storage: Storage<ReportId, ReportData>) {
    private val log = logger()

    val reports = ListObservableImpl(fetchReports())

    private fun fetchReports(): List<Report> = log.time("Fetching reports...", { time, reports -> "${reports.size} reports in $time" }) {
        storage.getAll().map { (id, data) -> Report(id, data) }
    }

    fun add(data: ReportData): Report {
        val updatedData = data.createdNow()
        val id = storage.add(updatedData)
        val report = Report(id, updatedData)
        reports += report
        return report
    }

    fun update(report: Report, data: ReportData): Report {
        val updatedData = data.updatedNow()
        storage.update(report.id, updatedData)
        val updatedReport = report.copy(data = updatedData)
        reports.replace(report, updatedReport)
        return updatedReport
    }

    fun delete(report: Report) {
        storage.delete(report.id)
        reports -= report
    }

    fun invalidate() {
        // Re-fetch all reports from storage
        reports.setAll(fetchReports())
    }
}