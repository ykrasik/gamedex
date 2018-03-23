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

package com.gitlab.ykrasik.gamedex

import java.io.File

/**
 * User: ykrasik
 * Date: 25/05/2016
 * Time: 11:29
 */
// TODO: Consider renaming this to GameSource or something, to allow for non-filesystem sources.
data class Library(
    val id: Int,
    val path: File,
    val data: LibraryData
) {
    val name get() = data.name
    val platform get() = data.platform

    override fun toString() = "[$platform] Library(id = $id, name = '$name', path = $path)"
}

data class LibraryData(
    val platform: Platform,
    val name: String
)

enum class Platform constructor(val displayName: String) {
    pc("PC"),
    mac("Mac"),
    android("Android"),
    excluded("Excluded");

    override fun toString() = displayName
}
