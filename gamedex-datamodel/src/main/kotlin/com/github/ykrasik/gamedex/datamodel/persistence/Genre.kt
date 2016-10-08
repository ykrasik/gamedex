package com.github.ykrasik.gamedex.datamodel.persistence

import com.github.ykrasik.gamedex.common.Id

/**
 * User: ykrasik
 * Date: 25/05/2016
 * Time: 11:23
 */
data class Genre(
    val id: Id<Genre>,
    val name: String
) : Comparable<Genre> {

    override fun compareTo(other: Genre) = name.compareTo(other.name)
}