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

import com.gitlab.ykrasik.gamedex.app.api.ViewManager
import com.gitlab.ykrasik.gamedex.app.api.report.ViewCanEditReport
import com.gitlab.ykrasik.gamedex.core.EventBus
import com.gitlab.ykrasik.gamedex.core.Presenter
import com.gitlab.ykrasik.gamedex.core.ViewSession
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 24/06/2018
 * Time: 18:05
 */
@Singleton
class ShowEditReportPresenter @Inject constructor(
    private val eventBus: EventBus,
    private val viewManager: ViewManager
) : Presenter<ViewCanEditReport> {
    override fun present(view: ViewCanEditReport) = object : ViewSession() {
        init {
            view.editReportActions.forEach { report ->
                val editReportView = viewManager.showEditReportView(report)
                eventBus.awaitViewFinished(editReportView)
                viewManager.closeEditReportView(editReportView)
            }
        }
    }
}