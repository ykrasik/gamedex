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

package com.gitlab.ykrasik.gamedex.app.api.task

import com.gitlab.ykrasik.gamedex.app.api.image.Image
import com.gitlab.ykrasik.gamedex.app.api.util.MultiReadChannel
import com.gitlab.ykrasik.gamedex.app.api.util.StatefulChannel
import kotlinx.coroutines.Job

/**
 * User: ykrasik
 * Date: 29/10/2018
 * Time: 22:49
 *
 * Supports displaying a single running task, with an optional single sub-task.
 */
interface TaskView {
    val job: StatefulChannel<Job?>

    val isCancellable: StatefulChannel<Boolean>
    val cancelTaskActions: MultiReadChannel<Unit>

    val taskProgress: TaskProgress
    val subTaskProgress: TaskProgress
    val isRunningSubTask: StatefulChannel<Boolean>

    fun taskSuccess(title: String, message: String)
    fun taskCancelled(title: String, message: String)
    fun taskError(title: String, error: Exception, message: String)
}

interface TaskProgress {
    val title: StatefulChannel<String>
    val image: StatefulChannel<Image?>
    val message: StatefulChannel<String>

    val processedItems: StatefulChannel<Int>
    val totalItems: StatefulChannel<Int>
    val progress: StatefulChannel<Double>
}