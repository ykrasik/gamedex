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

import com.gitlab.ykrasik.gamedex.util.EnumIdConverter
import com.gitlab.ykrasik.gamedex.util.IdentifiableEnum
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
}

data class LibraryData(
    val platform: Platform,
    val name: String
)

enum class Platform constructor(override val key: String) : IdentifiableEnum<String> {
    pc("PC"),
    mac("Mac"),
    android("Android"),
    xbox360("Xbox 360"),
    xboxOne("Xbox One"),
    ps3("PlayStation 3"),
    ps4("PlayStation 4"),
    excluded("Excluded");

    override fun toString() = key

    companion object {
        private val values = EnumIdConverter(Platform::class.java)

        operator fun invoke(name: String): Platform = values[name]
    }
}
