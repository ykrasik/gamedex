package com.github.ykrasik.gamedex.common

/**
 * User: ykrasik
 * Date: 08/10/2016
 * Time: 11:18
 */
data class Id<T>(val id: Int) {
    override fun toString() = id.toString()
}

fun <T> Int.toId(): Id<T> = Id(this)