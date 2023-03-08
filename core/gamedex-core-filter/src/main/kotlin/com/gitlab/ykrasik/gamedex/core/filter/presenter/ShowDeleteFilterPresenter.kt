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

package com.gitlab.ykrasik.gamedex.core.filter.presenter

import com.gitlab.ykrasik.gamedex.app.api.ViewManager
import com.gitlab.ykrasik.gamedex.app.api.filter.ViewCanDeleteFilter
import com.gitlab.ykrasik.gamedex.core.view.Presenter
import com.gitlab.ykrasik.gamedex.core.view.ViewService
import com.gitlab.ykrasik.gamedex.core.view.ViewSession
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 13/01/2019
 * Time: 09:49
 */
@Singleton
class ShowDeleteFilterPresenter @Inject constructor(
    private val viewService: ViewService,
) : Presenter<ViewCanDeleteFilter> {
    override fun present(view: ViewCanDeleteFilter) = object : ViewSession() {
        init {
            view::deleteNamedFilterActions.forEach { filter ->
                viewService.showAndHide(ViewManager::showDeleteFilterView, ViewManager::hide, filter)
            }
        }
    }
}