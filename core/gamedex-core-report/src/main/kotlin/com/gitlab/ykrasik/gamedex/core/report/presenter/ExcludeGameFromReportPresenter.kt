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

import com.gitlab.ykrasik.gamedex.app.api.report.ViewCanExcludeGameFromReport
import com.gitlab.ykrasik.gamedex.app.api.task.TaskRunner
import com.gitlab.ykrasik.gamedex.core.Presentation
import com.gitlab.ykrasik.gamedex.core.Presenter
import com.gitlab.ykrasik.gamedex.core.report.ReportService
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 26/06/2018
 * Time: 09:33
 */
@Singleton
class ExcludeGameFromReportPresenter @Inject constructor(
    private val reportService: ReportService,
    private val taskRunner: TaskRunner
) : Presenter<ViewCanExcludeGameFromReport> {
    override fun present(view: ViewCanExcludeGameFromReport) = object : Presentation() {
        init {
            view.excludeGameActions.forEach { (report, game) ->
                taskRunner.runTask(
                    reportService.update(report, report.data.copy(excludedGames = report.data.excludedGames + game.id))
                )
            }
        }
    }
}