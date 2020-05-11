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

package com.gitlab.ykrasik.gamedex.app.javafx.filter

import com.gitlab.ykrasik.gamedex.app.api.filter.DeleteFilterView
import com.gitlab.ykrasik.gamedex.app.api.filter.NamedFilter
import com.gitlab.ykrasik.gamedex.app.api.util.fromView
import com.gitlab.ykrasik.gamedex.app.api.util.writeTo
import com.gitlab.ykrasik.gamedex.javafx.control.prettyScrollPane
import com.gitlab.ykrasik.gamedex.javafx.screenBounds
import com.gitlab.ykrasik.gamedex.javafx.theme.Icons
import com.gitlab.ykrasik.gamedex.javafx.view.ConfirmationWindow
import com.gitlab.ykrasik.gamedex.javafx.viewMutableStateFlow
import tornadofx.stringBinding

/**
 * User: ykrasik
 * Date: 13/01/2019
 * Time: 13:13
 */
class JavaFxDeleteFilterView : ConfirmationWindow(icon = Icons.delete), DeleteFilterView {
    private val filterView = JavaFxFilterView(allowSaveLoad = false, readOnly = true)

    override val filter = viewMutableStateFlow(NamedFilter.Null, debugName = "filter")
        .writeTo(filterView.filter) { it.value.filter.fromView }

    init {
        titleProperty.bind(filter.property.stringBinding { "Delete filter '${it!!.id}'?" })
        register()
    }

    override val root = buildAreYouSure {
        prettyScrollPane {
            maxHeight = screenBounds.height / 2
            isFitToWidth = true
            isFitToHeight = true
            content = filterView.root
        }
    }
}