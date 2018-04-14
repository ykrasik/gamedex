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

package com.gitlab.ykrasik.gamedex.javafx.game.search

import com.gitlab.ykrasik.gamedex.javafx.BaseFragmentTestApp
import com.gitlab.ykrasik.gamedex.provider.ProviderSearchResult
import com.gitlab.ykrasik.gamedex.core.provider.SearchChooser
import com.gitlab.ykrasik.gamedex.test.*

/**
 * User: ykrasik
 * Date: 12/03/2017
 * Time: 13:05
 */
object ChooseSearchResultFragmentTestApp : BaseFragmentTestApp() {
    override fun init() {
        fun randomSearchResult() = ProviderSearchResult(
            name = randomName(),
            releaseDate = randomLocalDateString(),
            criticScore = randomScore(),
            userScore = randomScore(),
            thumbnailUrl = randomUrl(),
            apiUrl = randomUrl()
        )

        val data = SearchChooser.Data(
            name = randomName(),
            path = randomFile(),
            platform = randomEnum(),
            providerId = testProviderIds.randomElement(),
            results = List(10) { randomSearchResult() },
            filteredResults = List(10) { randomSearchResult() }
        )
        println("Result: " + SearchResultsFragment(data).show())
    }

    @JvmStatic fun main(args: Array<String>) {}
}