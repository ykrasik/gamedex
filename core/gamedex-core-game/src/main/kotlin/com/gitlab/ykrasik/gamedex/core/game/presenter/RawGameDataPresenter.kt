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

import com.gitlab.ykrasik.gamedex.app.api.game.RawGameDataView
import com.gitlab.ykrasik.gamedex.core.EventBus
import com.gitlab.ykrasik.gamedex.core.view.Presenter
import com.gitlab.ykrasik.gamedex.core.view.ViewSession
import com.gitlab.ykrasik.gamedex.util.mapFromJson
import com.gitlab.ykrasik.gamedex.util.toJsonStr
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 01/06/2020
 * Time: 09:13
 */
@Singleton
class RawGameDataPresenter @Inject constructor(
    private val eventBus: EventBus,
) : Presenter<RawGameDataView> {
    override fun present(view: RawGameDataView) = object : ViewSession() {
        init {
            view::rawGameData *= view.game.onlyChangesFromView().map {
                val json = it.rawGame.toJsonStr()
                json.mapFromJson<String, Any>()
            }

            view::acceptActions.forEach { hideView() }
        }

        private fun hideView() {
            eventBus.requestHideView(view)
        }
    }
}