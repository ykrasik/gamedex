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

package com.gitlab.ykrasik.gamedex.core.genre

import com.gitlab.ykrasik.gamedex.Genre
import com.gitlab.ykrasik.gamedex.GenreId
import com.gitlab.ykrasik.gamedex.core.settings.GameSettingsRepository
import com.gitlab.ykrasik.gamedex.core.util.toMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 06/10/2019
 * Time: 12:08
 */
@Singleton
class GenreServiceImpl @Inject constructor(
    genreRepo: GenreRepository,
    private val genreMappingRepo: GenreMappingRepository,
    private val gameSettingsRepository: GameSettingsRepository
) : GenreService {
    override val genres = genreRepo.genres
    private val genresById = genres.toMap(Genre::id)

    override fun get(id: GenreId) = genresById.getOrElse(id) { Genre.default(id) }

    override fun processGenres(genres: List<GenreId>): List<GenreId> {
        return genres.flatMap(::mapGenre).distinct().take(gameSettingsRepository.maxGenres.value)
    }

    private fun mapGenre(genreId: GenreId): List<GenreId> =
        genreMappingRepo.mapping.getOrElse(genreId) { listOf(genreId) }
}