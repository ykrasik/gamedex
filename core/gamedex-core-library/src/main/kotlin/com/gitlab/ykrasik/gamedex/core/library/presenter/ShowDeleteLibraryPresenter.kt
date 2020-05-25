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

package com.gitlab.ykrasik.gamedex.core.library.presenter

import com.gitlab.ykrasik.gamedex.app.api.ViewManager
import com.gitlab.ykrasik.gamedex.app.api.library.DeleteLibraryView
import com.gitlab.ykrasik.gamedex.app.api.library.ViewCanDeleteLibrary
import com.gitlab.ykrasik.gamedex.core.CommonData
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
 * Date: 01/06/2018
 * Time: 10:44
 */
@Singleton
class ShowDeleteLibraryPresenter @Inject constructor(
    private val commonData: CommonData,
    private val viewManager: ViewManager,
    eventBus: EventBus
) : Presenter<ViewCanDeleteLibrary> {
    init {
        flowScope(Dispatchers.Main.immediate) {
            eventBus.hideViewRequests<DeleteLibraryView>().forEach(debugName = "hideDeleteLibraryView") { viewManager.hide(it) }
        }
    }

    override fun present(view: ViewCanDeleteLibrary) = object : ViewSession() {
        init {
            view::canDeleteLibraries *= commonData.disableWhenGameSyncIsRunning

            view.deleteLibraryActions.forEach(debugName = "showDeleteLibraryView") { library ->
                view.canDeleteLibraries.assert()

                viewManager.showDeleteLibraryView(library)
            }
        }
    }
}