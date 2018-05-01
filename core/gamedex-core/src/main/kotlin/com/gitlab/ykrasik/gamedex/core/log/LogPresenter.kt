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

package com.gitlab.ykrasik.gamedex.core.log

import com.gitlab.ykrasik.gamedex.app.api.log.LogPresenter
import com.gitlab.ykrasik.gamedex.app.api.log.LogView
import com.gitlab.ykrasik.gamedex.app.api.task.TaskRunner
import com.gitlab.ykrasik.gamedex.core.BasePresenter
import com.gitlab.ykrasik.gamedex.core.general.GeneralUserConfig
import com.gitlab.ykrasik.gamedex.core.userconfig.UserConfigRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 26/04/2018
 * Time: 10:28
 */
@Singleton
class LogPresenterImpl @Inject constructor(
    userConfigRepository: UserConfigRepository,
    taskRunner: TaskRunner
) : BasePresenter<LogView>(taskRunner), LogPresenter {
    private val generalUserConfig = userConfigRepository[GeneralUserConfig::class]

    override fun initView(view: LogView) {
        view.entries = GamedexLog.entries
        generalUserConfig.logFilterLevelSubject.subscribe {
            view.level = it
        }
        generalUserConfig.logTailSubject.subscribe {
            view.logTail = it
        }
    }

    override fun onLevelChanged(level: String) {
        generalUserConfig.logFilterLevel = level
    }

    override fun onLogTailChanged(logTail: Boolean) {
        generalUserConfig.logTail = logTail
    }
}