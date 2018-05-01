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

import com.gitlab.ykrasik.gamedex.app.api.general.ClearUserDataPresenter
import com.gitlab.ykrasik.gamedex.app.api.general.ClearUserDataPresenterFactory
import com.gitlab.ykrasik.gamedex.app.api.general.ViewCanClearUserData
import com.gitlab.ykrasik.gamedex.app.api.task.TaskRunner
import com.gitlab.ykrasik.gamedex.core.launchOnUi
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 06/05/2018
 * Time: 13:15
 */
@Singleton
class ClearUserDataPresenterFactoryImpl @Inject constructor(
    private val generalSettingsService: GeneralSettingsService,
    private val taskRunner: TaskRunner
): ClearUserDataPresenterFactory {
    override fun present(view: ViewCanClearUserData): ClearUserDataPresenter = object : ClearUserDataPresenter {
        override fun clearUserData() = launchOnUi {
            taskRunner.runTask(generalSettingsService.deleteAllUserData())
        }
    }
}