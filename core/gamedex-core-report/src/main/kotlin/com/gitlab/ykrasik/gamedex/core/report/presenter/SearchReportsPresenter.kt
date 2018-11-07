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

package com.gitlab.ykrasik.gamedex.core.report.presenter

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.app.api.report.ViewCanSearchReports
import com.gitlab.ykrasik.gamedex.core.Presenter
import com.gitlab.ykrasik.gamedex.core.ViewSession
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 07/10/2018
 * Time: 09:52
 */
@Singleton
class SearchReportsPresenter @Inject constructor() : Presenter<ViewCanSearchReports> {
    override fun present(view: ViewCanSearchReports) = object : ViewSession() {
        init {
            view.searchTextChanges.forEach { onSearchTextChanged(it) }
        }

        private fun onSearchTextChanged(searchText: String) {
            if (searchText.isEmpty()) return
            view.matchingGame = view.result.games.firstOrNull { it.matchesSearchQuery(searchText) }
        }
    }

    // TODO: Do I need the better search capabilities of searchService?
    private fun Game.matchesSearchQuery(query: String) =
        query.isEmpty() || query.split(" ").all { word -> name.contains(word, ignoreCase = true) }
}