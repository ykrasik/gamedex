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

package com.gitlab.ykrasik.gamedex.app.javafx.report

import com.gitlab.ykrasik.gamedex.app.api.file.ViewCanBrowsePath
import com.gitlab.ykrasik.gamedex.app.api.report.*
import com.gitlab.ykrasik.gamedex.app.api.util.channel
import com.gitlab.ykrasik.gamedex.javafx.control.verticalGap
import com.gitlab.ykrasik.gamedex.javafx.perform
import com.gitlab.ykrasik.gamedex.javafx.state
import com.gitlab.ykrasik.gamedex.javafx.theme.*
import com.gitlab.ykrasik.gamedex.javafx.view.PresentableView
import javafx.geometry.Pos
import tornadofx.*
import java.io.File

/**
 * User: ykrasik
 * Date: 13/01/2019
 * Time: 09:15
 */
class ReportMenu : PresentableView("Reports", Icons.chart),
    ViewWithReports,
    ViewCanShowReport,
    ViewCanAddReport,
    ViewCanEditReport,
    ViewCanDeleteReport,
    ViewCanBrowsePath {

    override val reports = mutableListOf<Report>().observable()

    override val showReportActions = channel<Report>()
    override val currentReport = state<Report?>(null)

    override val addReportActions = channel<Unit>()
    override val editReportActions = channel<Report>()
    override val deleteReportActions = channel<Report>()

    override val browsePathActions = channel<File>()

    init {
        register()
    }

    override val root = vbox(spacing = 5) {
        addButton {
            alignment = Pos.CENTER
            useMaxWidth = true
            removeClass(GameDexStyle.toolbarButton)
            tooltip("Add a new report")
            action(addReportActions)
        }
        verticalGap()
        vbox {
            reports.perform { reports ->
                replaceChildren {
                    gridpane {
                        hgap = 5.0
                        vgap = 3.0
                        usePrefWidth = true
                        reports.forEach { report ->
                            row {
                                infoButton(report.name, Icons.chart) {
                                    useMaxWidth = true
                                    alignment = Pos.CENTER_LEFT
                                    action(showReportActions) { report }
                                }
                                editButton {
                                    removeClass(GameDexStyle.toolbarButton)
                                    action(editReportActions) { report }
                                }
                                deleteButton {
                                    removeClass(GameDexStyle.toolbarButton)
                                    action(deleteReportActions) { report }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}