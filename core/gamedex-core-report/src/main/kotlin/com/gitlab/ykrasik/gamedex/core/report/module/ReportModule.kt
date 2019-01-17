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

package com.gitlab.ykrasik.gamedex.core.report.module

import com.gitlab.ykrasik.gamedex.app.api.report.ReportData
import com.gitlab.ykrasik.gamedex.app.api.report.ReportId
import com.gitlab.ykrasik.gamedex.core.module.InternalCoreModule
import com.gitlab.ykrasik.gamedex.core.report.ReportService
import com.gitlab.ykrasik.gamedex.core.report.ReportServiceImpl
import com.gitlab.ykrasik.gamedex.core.report.presenter.*
import com.gitlab.ykrasik.gamedex.core.storage.IntIdJsonStorageFactory
import com.gitlab.ykrasik.gamedex.core.storage.Storage
import com.google.inject.Provides
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 18/09/2018
 * Time: 22:29
 */
object ReportModule : InternalCoreModule() {
    override fun configure() {
        bind(ReportService::class.java).to(ReportServiceImpl::class.java)

        bindPresenter(DeleteReportPresenter::class)
        bindPresenter(EditReportPresenter::class)
        bindPresenter(ExcludeGameFromReportPresenter::class)
        bindPresenter(ReportPresenter::class)
        bindPresenter(ReportsPresenter::class)
        bindPresenter(SearchReportResultPresenter::class)
        bindPresenter(ShowAddReportPresenter::class)
        bindPresenter(ShowDeleteReportPresenter::class)
        bindPresenter(ShowEditReportPresenter::class)
        bindPresenter(ShowReportPresenter::class)
    }

    @Provides
    @Singleton
    fun reportConfigStorage(): Storage<ReportId, ReportData> =
        IntIdJsonStorageFactory("data/reports")
}