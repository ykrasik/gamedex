package com.gitlab.ykrasik.gamedex.datamodel

import java.io.File

/**
 * User: ykrasik
 * Date: 25/05/2016
 * Time: 11:29
 */
data class Library(
    val id: Int,
    val path: File,
    val data: LibraryData
) : Comparable<Library> {

    val name: String get() = data.name
    val platform: GamePlatform get() = data.platform

    override fun compareTo(other: Library) = name.compareTo(other.name)
}

data class LibraryData(
    val platform: GamePlatform,
    val name: String
)