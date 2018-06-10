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

package com.gitlab.ykrasik.gamedex.core.general

import com.gitlab.ykrasik.gamedex.app.api.general.ClearUserDataView
import com.gitlab.ykrasik.gamedex.app.api.task.TaskRunner
import com.gitlab.ykrasik.gamedex.core.Presenter
import com.gitlab.ykrasik.gamedex.core.PresenterFactory
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 06/05/2018
 * Time: 13:15
 */
@Singleton
class ClearUserDataPresenterFactory @Inject constructor(
    private val databaseActionsService: DatabaseActionsService,
    private val taskRunner: TaskRunner
) : PresenterFactory<ClearUserDataView> {
    override fun present(view: ClearUserDataView) = object : Presenter() {
        init {
            view.clearUserDataActions.actionOnUi { clearUserData() }
        }

        private suspend fun clearUserData() {
            if (view.confirmClearUserData()) {
                taskRunner.runTask(databaseActionsService.deleteAllUserData())
            }
        }
    }
}