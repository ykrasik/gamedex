package com.github.ykrasik.gamedex.datamodel

import com.github.ykrasik.gamedex.common.Id
import com.github.ykrasik.gamedex.common.delegate
import java.nio.file.Path

/**
 * User: ykrasik
 * Date: 25/05/2016
 * Time: 11:29
 */
data class Library(val id: Id<Library>, val data: LibraryData) : Comparable<Library> {
    override fun compareTo(other: Library) = data.compareTo(other.data)

    val name by delegate(data, LibraryData::name)
    val path by delegate(data, LibraryData::path)
    val platform by delegate(data, LibraryData::platform)
}

data class LibraryData(
    val path: Path,
    val name: String,
    val platform: GamePlatform
) : Comparable<LibraryData> {

    override fun compareTo(other: LibraryData) = name.compareTo(other.name)
}