package com.github.ykrasik.gamedex.datamodel

import com.github.ykrasik.gamedex.common.Id
import java.nio.file.Path

/**
 * User: ykrasik
 * Date: 25/05/2016
 * Time: 11:29
 */
data class Library(
    val id: Id<Library>,
    val path: Path,
    val platform: GamePlatform,
    val name: String
) : Comparable<Library> {

    override fun compareTo(other: Library) = name.compareTo(other.name)
}