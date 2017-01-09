package com.github.ykrasik.gamedex.datamodel

/**
 * User: ykrasik
 * Date: 06/01/2017
 * Time: 20:22
 */
class GameImage(
    val id: GameImageId,
    val url: String?,
    val bytes: ByteArray?
) {
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