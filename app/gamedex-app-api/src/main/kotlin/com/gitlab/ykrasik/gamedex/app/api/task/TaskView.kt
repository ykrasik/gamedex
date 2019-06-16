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

package com.gitlab.ykrasik.gamedex.app.api.task

import com.gitlab.ykrasik.gamedex.app.api.image.Image
import com.gitlab.ykrasik.gamedex.app.api.util.MultiReceiveChannel
import com.gitlab.ykrasik.gamedex.app.api.util.State
import kotlinx.coroutines.Job

/**
 * User: ykrasik
 * Date: 29/10/2018
 * Time: 22:49
 *
 * Supports displaying a single running task, with an optional single sub-task.
 */
interface TaskView {
    val job: State<Job?>

    val isCancellable: State<Boolean>
    val cancelTaskActions: MultiReceiveChannel<Unit>

    val taskProgress: TaskProgress
    val subTaskProgress: TaskProgress
    val isRunningSubTask: State<Boolean>

    fun taskSuccess(message: String)
    fun taskCancelled(message: String)
    fun taskError(error: Exception, message: String)
}

interface TaskProgress {
    val title: State<String>
    val image: State<Image?>
    val message: State<String>

    val processedItems: State<Int>
    val totalItems: State<Int>
    val progress: State<Double>
}