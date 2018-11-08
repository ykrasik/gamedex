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

package com.gitlab.ykrasik.gamedex.app.api.task

import com.gitlab.ykrasik.gamedex.app.api.image.Image
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.channels.ReceiveChannel

/**
 * User: ykrasik
 * Date: 29/10/2018
 * Time: 22:49
 *
 * Supports displaying a single running task, with an optional single sub-task.
 */
interface TaskView {
    var job: Job?

    var isCancellable: Boolean
    val cancelTaskActions: ReceiveChannel<Unit>

    val taskProgress: TaskProgress
    val subTaskProgress: TaskProgress
    var isRunningSubTask: Boolean

    fun taskSuccess(message: String)
    fun taskCancelled(message: String)
}

interface TaskProgress {
    var title: String
    var image: Image?
    var message: String

    var processedItems: Int
    var totalItems: Int
    var progress: Double
}