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

package com.gitlab.ykrasik.gamedex.core.game.presenter

import com.gitlab.ykrasik.gamedex.app.api.game.ViewWithCurrentPlatformFilter
import com.gitlab.ykrasik.gamedex.core.game.CurrentPlatformFilterRepository
import com.gitlab.ykrasik.gamedex.core.view.Presenter
import com.gitlab.ykrasik.gamedex.core.view.ViewSession
import kotlinx.coroutines.flow.drop
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 11/12/2018
 * Time: 08:53
 */
@Singleton
class CurrentPlatformFilterPresenter @Inject constructor(
    private val repo: CurrentPlatformFilterRepository,
) : Presenter<ViewWithCurrentPlatformFilter> {
    override fun present(view: ViewWithCurrentPlatformFilter) = object : ViewSession() {
        init {
            view::currentPlatformFilter *= repo.currentPlatformFilter
            view.currentPlatformFilterValidatedValue.allValues()
                .drop(1)
                .forEach(debugName = "onCurrentPlatformFilterValidatedValueChanged") { (filter, isValid) ->
                    if (isValid.isSuccess) {
                        repo.update(filter)
                    }
                }
        }
    }
}