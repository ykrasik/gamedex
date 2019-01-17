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

import com.gitlab.ykrasik.gamedex.app.api.report.DeleteReportView
import com.gitlab.ykrasik.gamedex.app.api.report.Report
import com.gitlab.ykrasik.gamedex.app.javafx.filter.JavaFxFilterView
import com.gitlab.ykrasik.gamedex.javafx.theme.Icons
import com.gitlab.ykrasik.gamedex.javafx.typeSafeOnChange
import com.gitlab.ykrasik.gamedex.javafx.view.ConfirmationWindow
import javafx.beans.property.SimpleObjectProperty
import tornadofx.getValue
import tornadofx.setValue
import tornadofx.stringBinding

/**
 * User: ykrasik
 * Date: 13/01/2019
 * Time: 13:13
 */
class JavaFxDeleteReportView : ConfirmationWindow(icon = Icons.delete), DeleteReportView {
    private val reportProperty = SimpleObjectProperty(Report.Null)
    override var report: Report by reportProperty

    private val filterView = JavaFxFilterView(onlyShowConditionsForCurrentPlatform = false)

    init {
        titleProperty.bind(reportProperty.stringBinding { "Delete report '${it!!.name}'?" })
        register()

        reportProperty.typeSafeOnChange {
            filterView.externalMutations.value = it.filter
        }
    }

    override val root = buildAreYouSure {
        isDisable = true
        add(filterView.root)
    }
}