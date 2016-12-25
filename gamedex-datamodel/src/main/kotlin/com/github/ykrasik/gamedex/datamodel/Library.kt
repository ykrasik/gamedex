package com.github.ykrasik.gamedex.datamodel

import com.github.ykrasik.gamedex.common.Id
import java.nio.file.Path

/**
 * User: ykrasik
 * Date: 25/05/2016
 * Time: 11:29
 */
data class Library(val id: Id<Library>, val data: LibraryData) : HasPath, Comparable<Library> {
    override fun compareTo(other: Library) = data.compareTo(other.data)

    // FIXME: Find a less verbose way
    val name: String get() = data.name
    override val path: Path get() = data.path
    val platform: GamePlatform get() = data.platform

    companion object {
        operator fun invoke(id: Id<Library>, path: Path, name: String, platform: GamePlatform): Library = Library(
            id, LibraryData(path, name, platform)
        )
    }
}

data class LibraryData(
    val path: Path,
    val name: String,
    val platform: GamePlatform
) : Comparable<LibraryData> {

    override fun compareTo(other: LibraryData) = name.compareTo(other.name)
}