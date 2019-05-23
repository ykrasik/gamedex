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

package com.gitlab.ykrasik.gamedex.core.game

import com.gitlab.ykrasik.gamedex.util.toMultiMap
import com.gitlab.ykrasik.gamedex.util.urlDecoded

/**
 * User: ykrasik
 * Date: 24/03/2018
 * Time: 11:52
 */
data class GameConfig(
    val maxGenres: Int,
    val maxScreenshots: Int,
    private val noGenre: String,
    private val genreReverseMapping: Map<String, List<String>>     // TODO: Make this configurable by the user.
) {
    private val genreMapping = run {
        val genres = genreReverseMapping.flatMap { (target, sources) ->
            sources.map { it to target.urlDecoded() }
        }.toMultiMap()
        genres.forEach { (source, targets) ->
            if (targets.contains(noGenre)) {
                require(targets.size == 1) { "Genre('$source') is both mapped to a value and marked as filtered: $targets" }
            }
        }
        genres
    }

    fun mapGenre(genre: String): List<String> = genreMapping[genre]?.let { if (it.contains(noGenre)) emptyList() else it } ?: listOf(genre)
}