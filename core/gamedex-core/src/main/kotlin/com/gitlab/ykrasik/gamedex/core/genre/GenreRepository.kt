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
import com.gitlab.ykrasik.gamedex.core.storage.Storage
import com.gitlab.ykrasik.gamedex.core.util.ListObservableImpl
import com.gitlab.ykrasik.gamedex.util.logger
import com.gitlab.ykrasik.gamedex.util.time
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 06/10/2019
 * Time: 13:22
 */
@Singleton
class GenreRepository @Inject constructor(private val storage: Storage<GenreId, Genre>) {
    private val log = logger()

    val genres = ListObservableImpl(fetchGenres())

    private fun fetchGenres(): List<Genre> =
        log.time("Fetching genres...", { time, genres -> "${genres.size} genres in $time" }) {
            storage.getAll().values.toList()
        }

    fun set(genre: Genre): Genre {
        val existingGenreIndex = genres.indexOfFirst { it.id == genre.id }
        val updatedGenre = if (existingGenreIndex != -1) {
            genre.updatedNow()
        } else {
            genre.createdNow()
        }
        storage[genre.id] = updatedGenre

        if (existingGenreIndex != -1) {
            genres[existingGenreIndex] = updatedGenre
        } else {
            genres += updatedGenre
        }
        return updatedGenre
    }

    fun delete(genre: Genre) {
        storage.delete(genre.id)
        genres -= genre
    }

    fun invalidate() {
        // Re-fetch all genres from storage
        genres.setAll(fetchGenres())
    }
}