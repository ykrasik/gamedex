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

package com.gitlab.ykrasik.gamedex.core.filter

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.GameId
import com.gitlab.ykrasik.gamedex.app.api.filter.Filter
import com.gitlab.ykrasik.gamedex.core.file.FileSystemService

/**
 * User: ykrasik
 * Date: 02/06/2018
 * Time: 16:43
 */
class FilterContextImpl(
    override val games: List<Game>,
    private val fileSystemService: FileSystemService
) : Filter.Context {
    private val cache = mutableMapOf<String, Any>()
    override val additionalData = mutableMapOf<GameId, MutableSet<Filter.Context.AdditionalData>>()

    override fun size(game: Game) = fileSystemService.structure(game).size

    override fun toFileName(name: String): String = fileSystemService.toFileName(name)

    @Suppress("UNCHECKED_CAST")
    override fun <T> cache(key: String, defaultValue: () -> T) = cache.getOrPut(key, defaultValue as () -> Any) as T

    override fun addAdditionalInfo(game: Game, rule: Filter.Rule, values: List<Any>) =
        values.forEach { addAdditionalInfo(game, rule, it) }

    override fun addAdditionalInfo(game: Game, rule: Filter.Rule, value: Any?) {
        val gameAdditionalInfo = additionalData.getOrPut(game.id) { mutableSetOf() }
        val additionalInfo = Filter.Context.AdditionalData(rule::class, value)
        if (!gameAdditionalInfo.contains(additionalInfo)) gameAdditionalInfo += additionalInfo
    }
}