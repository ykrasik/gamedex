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

package com.gitlab.ykrasik.gamedex.core.library.presenter

import com.gitlab.ykrasik.gamedex.app.api.ViewManager
import com.gitlab.ykrasik.gamedex.app.api.library.ViewCanAddLibrary
import com.gitlab.ykrasik.gamedex.core.EventBus
import com.gitlab.ykrasik.gamedex.core.Presenter
import com.gitlab.ykrasik.gamedex.core.ViewSession
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 01/06/2018
 * Time: 10:33
 */
@Singleton
class ShowAddLibraryPresenter @Inject constructor(
    private val viewManager: ViewManager,
    private val eventBus: EventBus
) : Presenter<ViewCanAddLibrary> {
    override fun present(view: ViewCanAddLibrary) = object : ViewSession() {
        init {
            view.addLibraryActions.forEach {
                val editLibraryView = viewManager.showEditLibraryView(library = null)
                eventBus.awaitViewFinished(editLibraryView)
                viewManager.closeEditLibraryView(editLibraryView)
            }
        }
    }
}