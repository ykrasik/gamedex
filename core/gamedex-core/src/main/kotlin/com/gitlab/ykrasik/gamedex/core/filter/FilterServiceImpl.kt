/****************************************************************************
 * Copyright (C) 2016-2019 Yevgeny Krasik                                   *
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

package com.gitlab.ykrasik.gamedex.core.filter

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.Platform
import com.gitlab.ykrasik.gamedex.app.api.filter.Filter
import com.gitlab.ykrasik.gamedex.core.provider.GameProviderService
import com.gitlab.ykrasik.gamedex.provider.ProviderId
import com.gitlab.ykrasik.gamedex.provider.id
import com.gitlab.ykrasik.gamedex.provider.supports
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 27/04/2019
 * Time: 18:25
 */
@Singleton
class FilterServiceImpl @Inject constructor(
    private val gameProviderService: GameProviderService
) : FilterService {
    override fun createContext(): Filter.Context = FilterContextImpl()

    override fun filter(games: List<Game>, filter: Filter): List<Game> {
        val context = createContext()
        return games.filter { filter.evaluate(it, context) }
    }

    private inner class FilterContextImpl : Filter.Context {
        override val now = com.gitlab.ykrasik.gamedex.util.now

        override fun providerSupports(providerId: ProviderId, platform: Platform) =
            gameProviderService.allProviders.find { it.id == providerId }!!.supports(platform)
    }
}