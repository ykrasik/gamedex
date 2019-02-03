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

package com.gitlab.ykrasik.gamedex

import java.io.File

/**
 * User: ykrasik
 * Date: 25/05/2016
 * Time: 11:29
 */
typealias LibraryId = Int

data class Library(
    val id: LibraryId,
    val data: LibraryData
) {
    val name get() = data.name
    val path get() = data.path
    val type get() = data.type
    val platform get() = if (type != LibraryType.Excluded) data.platform!! else throw IllegalStateException("Library($id) has no platform because it is of type($type)!")
    val platformOrNull get() = data.platform

    companion object {
        val Null = Library(
            id = 0,
            data = LibraryData(
                name = "",
                path = File(""),
                type = LibraryType.Excluded,
                platform = null
            )
        )
    }
}

data class LibraryData(
    val name: String,
    val path: File,
    val type: LibraryType,
    val platform: Platform?
)

enum class LibraryType(val displayName: String) {
    Digital("Digital"),
    Excluded("Excluded")
}

enum class Platform(val displayName: String) {
    Windows("Windows"),
    Linux("Linux"),
    Mac("Mac"),
    Android("Android")
}

data class LibraryPath(val library: Library, val path: File) {
    val relativePath: File get() = path.relativeTo(library.path)

    companion object {
        val Null = LibraryPath(Library.Null, File(""))
    }
}