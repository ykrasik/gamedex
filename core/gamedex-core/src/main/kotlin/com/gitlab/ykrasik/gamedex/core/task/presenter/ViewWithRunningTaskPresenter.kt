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

package com.gitlab.ykrasik.gamedex.core.task.presenter

import com.gitlab.ykrasik.gamedex.app.api.task.ViewWithRunningTask
import com.gitlab.ykrasik.gamedex.core.EventBus
import com.gitlab.ykrasik.gamedex.core.Presenter
import com.gitlab.ykrasik.gamedex.core.ViewSession
import com.gitlab.ykrasik.gamedex.core.task.TaskFinishedEvent
import com.gitlab.ykrasik.gamedex.core.task.TaskStartedEvent
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 29/10/2018
 * Time: 12:27
 */
@Singleton
class ViewWithRunningTaskPresenter @Inject constructor(private val eventBus: EventBus) : Presenter<ViewWithRunningTask> {
    override fun present(view: ViewWithRunningTask) = object : ViewSession() {
        private var isRunningTask by view.isRunningTask

        init {
            eventBus.forEach<TaskStartedEvent<*>> { isRunningTask = true }
            eventBus.forEach<TaskFinishedEvent<*>> { isRunningTask = false }
        }
    }
}