/****************************************************************************
 * Copyright (C) 2016-2020 Yevgeny Krasik                                   *
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

import com.gitlab.ykrasik.gamedex.app.api.provider.BulkUpdateGamesView
import com.gitlab.ykrasik.gamedex.app.javafx.filter.JavaFxFilterView
import com.gitlab.ykrasik.gamedex.javafx.addComponent
import com.gitlab.ykrasik.gamedex.javafx.control.prettyScrollPane
import com.gitlab.ykrasik.gamedex.javafx.screenBounds
import com.gitlab.ykrasik.gamedex.javafx.theme.Icons
import com.gitlab.ykrasik.gamedex.javafx.view.ConfirmationWindow
import tornadofx.borderpane
import tornadofx.paddingAll

/**
 * User: ykrasik
 * Date: 05/06/2017
 * Time: 10:57
 */
class JavaFxBulkUpdateGamesView : ConfirmationWindow("Bulk Update Games", Icons.download), BulkUpdateGamesView {
    private val filterView = JavaFxFilterView()

    override val bulkUpdateGamesFilter = filterView.filter
    override val bulkUpdateGamesFilterIsValid = filterView.filterIsValid

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
}