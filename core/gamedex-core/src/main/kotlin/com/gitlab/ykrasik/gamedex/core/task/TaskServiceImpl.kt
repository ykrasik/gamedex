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

package com.gitlab.ykrasik.gamedex.core.task

import com.gitlab.ykrasik.gamedex.app.api.ViewManager
import com.gitlab.ykrasik.gamedex.core.EventBus
import com.gitlab.ykrasik.gamedex.core.awaitEvent
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 29/10/2018
 * Time: 22:55
 */
@Singleton
class TaskServiceImpl @Inject constructor(
    private val viewManager: ViewManager,
    private val eventBus: EventBus,
) : TaskService {
    private val mutex = Mutex()

    override suspend fun <T> execute(task: Task<T>): T = mutex.withLock {
        val view = viewManager.showTaskView()
        try {
            eventBus.send(TaskEvent.RequestStart(task))
            val event = eventBus.awaitEvent<TaskEvent.Finished<T>> { it.task === task }
            event.result.getOrThrow()
        } finally {
            viewManager.hide(view)
        }
    }
}