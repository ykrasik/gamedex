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

package com.gitlab.ykrasik.gamedex.core.game.discover

import com.gitlab.ykrasik.gamedex.app.api.game.discover.DiscoverGameChoiceConfigPresenter
import com.gitlab.ykrasik.gamedex.app.api.game.discover.DiscoverGameChoiceConfigView
import com.gitlab.ykrasik.gamedex.app.api.game.discover.DiscoverGameChooseResults
import com.gitlab.ykrasik.gamedex.core.BasePresenter
import com.gitlab.ykrasik.gamedex.core.game.GameUserConfig
import com.gitlab.ykrasik.gamedex.core.userconfig.UserConfigRepository
import javax.inject.Inject

/**
 * User: ykrasik
 * Date: 29/04/2018
 * Time: 14:25
 *
 * Not a singleton - there's multiple instances of the view.
 */
class DiscoverGameChoiceConfigPresenterImpl @Inject constructor(
    userConfigRepository: UserConfigRepository
) : BasePresenter<DiscoverGameChoiceConfigView.Event, DiscoverGameChoiceConfigView>(), DiscoverGameChoiceConfigPresenter {
    private val gameUserConfig = userConfigRepository[GameUserConfig::class]

    override fun initView(view: DiscoverGameChoiceConfigView) {
        gameUserConfig.discoverGameChooseResultsSubject.subscribe {
            view.discoverGameChooseResults = it
        }
    }

    override suspend fun handleEvent(event: DiscoverGameChoiceConfigView.Event) = when (event) {
        is DiscoverGameChoiceConfigView.Event.DiscoverGameChoiceChanged -> handleDiscoverGameChoiceChanged(event.discoverGameChooseResults)
    }

    private fun handleDiscoverGameChoiceChanged(discoverGameChooseResults: DiscoverGameChooseResults) {
        gameUserConfig.discoverGameChooseResults = discoverGameChooseResults
    }
}