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

package com.gitlab.ykrasik.gamedex.core.filter.presenter

import com.gitlab.ykrasik.gamedex.app.api.ViewManager
import com.gitlab.ykrasik.gamedex.app.api.filter.EditFilterView
import com.gitlab.ykrasik.gamedex.app.api.filter.ViewCanAddOrEditFilter
import com.gitlab.ykrasik.gamedex.core.EventBus
import com.gitlab.ykrasik.gamedex.core.util.flowScope
import com.gitlab.ykrasik.gamedex.core.view.Presenter
import com.gitlab.ykrasik.gamedex.core.view.ViewSession
import com.gitlab.ykrasik.gamedex.core.view.hideViewRequests
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 24/06/2018
 * Time: 18:08
 */
@Singleton
class ShowAddOrEditFilterPresenter @Inject constructor(
    private val viewManager: ViewManager,
    eventBus: EventBus
) : Presenter<ViewCanAddOrEditFilter> {
    init {
        flowScope(Dispatchers.Main.immediate) {
            eventBus.hideViewRequests<EditFilterView>().forEach(debugName = "hideEditFilterView") { viewManager.hide(it) }
        }
    }

    override fun present(view: ViewCanAddOrEditFilter) = object : ViewSession() {
        init {
            view.addOrEditFilterActions.forEach(debugName = "showEditFilterView") {
                viewManager.showEditFilterView(it)
            }
        }
    }
}