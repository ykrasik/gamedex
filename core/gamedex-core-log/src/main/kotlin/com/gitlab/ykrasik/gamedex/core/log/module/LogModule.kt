/****************************************************************************
 * Copyright (C) 2016-2023 Yevgeny Krasik                                   *
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

package com.gitlab.ykrasik.gamedex.core.log.module

import com.gitlab.ykrasik.gamedex.core.log.presenter.LogLevelPresenter
import com.gitlab.ykrasik.gamedex.core.log.presenter.LogViewPresenter
import com.gitlab.ykrasik.gamedex.core.log.presenter.ShowLogViewPresenter
import com.gitlab.ykrasik.gamedex.core.module.InternalCoreModule

/**
 * User: ykrasik
 * Date: 12/10/2018
 * Time: 10:40
 */
object LogModule : InternalCoreModule() {
    override fun configure() {
        bindPresenter(LogViewPresenter::class)
        bindPresenter(LogLevelPresenter::class)
        bindPresenter(ShowLogViewPresenter::class)
    }
}