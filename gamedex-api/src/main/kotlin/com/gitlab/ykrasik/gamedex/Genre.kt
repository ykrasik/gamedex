/****************************************************************************
 * Copyright (C) 2016-2023 Yevgeny Krasik                                   *
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

package com.gitlab.ykrasik.gamedex

/**
 * User: ykrasik
 * Date: 06/10/2019
 * Time: 10:41
 */
typealias GenreId = String

data class Genre(
    val id: GenreId,
    val color: String?,
    val timestamp: Timestamp,
) {
    fun createdNow() = copy(timestamp = Timestamp.now)
    fun updatedNow() = copy(timestamp = timestamp.updatedNow())

    companion object {
        val Null = default("")

        fun default(id: GenreId) = Genre(
            id = id,
            color = null,
            timestamp = Timestamp.Null
        )
    }
}