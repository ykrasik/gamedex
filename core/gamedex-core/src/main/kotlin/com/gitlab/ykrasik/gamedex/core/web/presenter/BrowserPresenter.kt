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

package com.gitlab.ykrasik.gamedex.core.web.presenter

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.app.api.web.ViewWithBrowser
import com.gitlab.ykrasik.gamedex.core.Presenter
import com.gitlab.ykrasik.gamedex.core.ViewSession
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 06/10/2018
 * Time: 12:14
 */
@Singleton
class BrowserPresenter @Inject constructor() : Presenter<ViewWithBrowser> {
    override fun present(view: ViewWithBrowser) = object : ViewSession() {
        init {
            view.gameChanges.forEach { game ->
                if (showing) {
                    browseToGame(game)
                }
            }
        }

        override fun onShow() {
            browseToGame(view.game)
        }

        override fun onHide() {
            browseToGame(null)
        }

        private fun browseToGame(game: Game?) {
            val url = game?.let {
                val search = URLEncoder.encode("${it.name} ${it.platform} gameplay", "utf-8")
                "https://www.youtube.com/results?search_query=$search"
            }
            view.browseTo(url)
        }
    }
}