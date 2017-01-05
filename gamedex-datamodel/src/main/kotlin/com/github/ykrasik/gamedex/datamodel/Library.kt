package com.github.ykrasik.gamedex.datamodel

import java.io.File

/**
 * User: ykrasik
 * Date: 25/05/2016
 * Time: 11:29
 */
data class Library(
    val id: Int,
    val data: LibraryData
) : Comparable<Library> {

    val name: String get() = data.name
    val path: File get() = data.path
    val platform: GamePlatform get() = data.platform

    override fun compareTo(other: Library) = data.compareTo(other.data)
}

data class LibraryData(
    val path: File,
    val name: String,
    val platform: GamePlatform
) : Comparable<LibraryData> {

    override fun compareTo(other: LibraryData) = name.compareTo(other.name)
}