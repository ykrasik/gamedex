package com.github.ykrasik.gamedex.datamodel

import java.util.*

/**
 * User: ykrasik
 * Date: 06/01/2017
 * Time: 20:22
 */
data class GameImage(
    val id: GameImageId,
    val url: String?,
    val bytes: ByteArray?
) {
    override fun equals(other: Any?): Boolean {
        if (other !is GameImage) return false
        return id == other.id && url == other.url && Arrays.equals(bytes, other.bytes)
    }

    override fun hashCode(): Int {
        return Objects.hash(id, url) + Objects.hash(bytes)
    }

    override fun toString() = "GameImage(id = $id, url = $url, bytes = ${if (bytes != null) "Present" else "Empty"})"
}

data class GameImageId(
    val gameId: Int,
    val type: GameImageType
)

enum class GameImageType {
    Thumbnail,
    Poster,
    Screenshot1,
    Screenshot2,
    Screenshot3,
    Screenshot4,
    Screenshot5,
    Screenshot6,
    Screenshot7,
    Screenshot8,
    Screenshot9,
    Screenshot10
}