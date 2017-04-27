package com.gitlab.ykrasik.gamedex

import com.gitlab.ykrasik.gamedex.util.EnumIdConverter
import com.gitlab.ykrasik.gamedex.util.IdentifiableEnum
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

    val name get() = data.name
    val platform get() = data.platform

    override fun compareTo(other: Library) = name.compareTo(other.name)
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
