/****************************************************************************
 * Copyright (C) 2016-2023 Yevgeny Krasik                                   *
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

package com.gitlab.ykrasik.gamedex.app.javafx.provider

import com.gitlab.ykrasik.gamedex.app.api.filter.Filter
import com.gitlab.ykrasik.gamedex.app.api.provider.BulkUpdateGamesView
import com.gitlab.ykrasik.gamedex.app.api.provider.UpdateGameProgressData
import com.gitlab.ykrasik.gamedex.app.api.util.ValidatedValue
import com.gitlab.ykrasik.gamedex.app.api.util.fromView
import com.gitlab.ykrasik.gamedex.app.api.util.writeFrom
import com.gitlab.ykrasik.gamedex.app.api.util.writeTo
import com.gitlab.ykrasik.gamedex.app.javafx.JavaFxViewManager
import com.gitlab.ykrasik.gamedex.app.javafx.filter.JavaFxFilterView
import com.gitlab.ykrasik.gamedex.javafx.addComponent
import com.gitlab.ykrasik.gamedex.javafx.control.prettyScrollPane
import com.gitlab.ykrasik.gamedex.javafx.screenBounds
import com.gitlab.ykrasik.gamedex.javafx.theme.Icons
import com.gitlab.ykrasik.gamedex.javafx.view.ConfirmationWindow
import com.gitlab.ykrasik.gamedex.javafx.viewMutableStateFlow
import com.gitlab.ykrasik.gamedex.util.IsValid
import tornadofx.borderpane
import tornadofx.paddingAll
import tornadofx.text

/**
 * User: ykrasik
 * Date: 05/06/2017
 * Time: 10:57
 */
class JavaFxBulkUpdateGamesView : ConfirmationWindow("Bulk Update Games", Icons.download), BulkUpdateGamesView {
    private val viewManager: JavaFxViewManager by inject()

    private val filterView = JavaFxFilterView()
    private val resumeBulkUpdateFilterView = JavaFxFilterView(allowSaveLoad = false, readOnly = true)

    override val bulkUpdateGamesFilter = viewMutableStateFlow(Filter.Null, debugName = "bulkUpdateGamesFilter")
        .writeTo(filterView.filter) { it.asFromView() }
        .writeFrom(filterView.filter) { it.asFromView() }

    override val bulkUpdateGamesFilterValidatedValue = viewMutableStateFlow(ValidatedValue(Filter.Null, IsValid.valid), debugName = "bulkUpdateGamesFilterValidatedValue")
        .writeFrom(filterView.filterValidatedValue) { it.fromView }

    init {
        register()
    }

    override val root = borderpane {
        maxHeight = screenBounds.height * 2 / 3
        top = confirmationToolbar()
        center = prettyScrollPane {
            addComponent(filterView) {
                paddingAll = 20
            }
        }
    }

    override suspend fun confirmResumeInProgressUpdate(progressData: UpdateGameProgressData): Boolean {
        resumeBulkUpdateFilterView.filter *= progressData.filter

        return viewManager.showAreYouSureDialog("Resume previously unfinished update?", Icons.information) {
            text("Remaining games: ${progressData.remainingGames.size}") {
                wrappingWidth = 400.0
            }
            prettyScrollPane {
                maxHeight = screenBounds.height / 2
                isFitToWidth = true
                isFitToHeight = true
                content = resumeBulkUpdateFilterView.root
            }
        }
    }
}