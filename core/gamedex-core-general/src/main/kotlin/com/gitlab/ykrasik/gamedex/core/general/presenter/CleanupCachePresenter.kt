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

package com.gitlab.ykrasik.gamedex.core.general.presenter

import com.gitlab.ykrasik.gamedex.app.api.general.CleanupCacheView
import com.gitlab.ykrasik.gamedex.core.Presenter
import com.gitlab.ykrasik.gamedex.core.ViewSession
import com.gitlab.ykrasik.gamedex.core.general.DatabaseActionsService
import com.gitlab.ykrasik.gamedex.core.task.TaskService
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 22/09/2018
 * Time: 14:35
 */
@Singleton
class CleanupCachePresenter @Inject constructor(
    private val databaseActionsService: DatabaseActionsService,
    private val taskService: TaskService
) : Presenter<CleanupCacheView> {
    override fun present(view: CleanupCacheView) = object : ViewSession() {
        init {
            view.cleanupCacheActions.forEach { cleanupCache() }
        }

        private suspend fun cleanupCache() {
            val staleCache = taskService.execute(databaseActionsService.detectStaleCache())
            if (staleCache.isEmpty) return

            if (view.confirmDeleteStaleCache(staleCache)) {
                taskService.execute(databaseActionsService.deleteStaleCache(staleCache))
            }
        }
    }
}