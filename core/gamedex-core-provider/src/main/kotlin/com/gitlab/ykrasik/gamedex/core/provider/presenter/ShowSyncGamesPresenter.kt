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

package com.gitlab.ykrasik.gamedex.core.provider.presenter

import com.gitlab.ykrasik.gamedex.app.api.ViewManager
import com.gitlab.ykrasik.gamedex.core.EventBus
import com.gitlab.ykrasik.gamedex.core.ViewSession
import com.gitlab.ykrasik.gamedex.core.awaitEvent
import com.gitlab.ykrasik.gamedex.core.provider.SyncGamesFinishedEvent
import com.gitlab.ykrasik.gamedex.core.provider.SyncGamesStartedEvent
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 31/12/2018
 * Time: 20:20
 */
@Singleton
class ShowSyncGamesPresenter @Inject constructor(
    private val eventBus: EventBus,
    private val viewManager: ViewManager
) {
    private val session = object : ViewSession() {
        init {
            eventBus.forEach<SyncGamesStartedEvent> {
                val view = viewManager.showSyncGamesView()
                eventBus.awaitEvent<SyncGamesFinishedEvent>()
                viewManager.hide(view)
            }
        }
    }
}