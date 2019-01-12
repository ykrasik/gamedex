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
    val platform get() = data.platform

    companion object {
        val Null = Library(
            id = 0,
            data = LibraryData(
                name = "",
                path = File(""),
                platform = Platform.pc
            )
        )
    }
}

data class LibraryData(
    val name: String,
    val path: File,
    val platform: Platform
)

enum class Platform(val displayName: String) {
    pc("PC"),
    mac("Mac"),
    android("Android"),
    excluded("Excluded");

    override fun toString() = displayName

    companion object {
        val realPlatforms = values().toList() - excluded
    }
}

data class LibraryPath(val library: Library, val path: File) {
    val relativePath: File get() = path.relativeTo(library.path)

    companion object {
        val Null = LibraryPath(Library.Null, File(""))
    }
}