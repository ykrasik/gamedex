package com.gitlab.ykrasik.gamedex.datamodel

import java.util.*

/**
 * User: ykrasik
 * Date: 06/01/2017
 * Time: 20:22
 */
data class GameImage(
    val id: Int,
    val url: String,
    val bytes: ByteArray?
) {
    override fun equals(other: Any?): Boolean {
        if (other !is GameImage) return false
        return id == other.id && url == other.url && Arrays.equals(bytes, other.bytes)
    }

    override fun hashCode(): Int {
        return Objects.hash(id, url) + Objects.hash(bytes)
    }

    override fun toString() = "GameImage(id=$id, url=$url, bytes=${if (bytes != null) "Present" else "Empty"})"
}

data class ImageUrls(
    val thumbnailUrl: String?,
    val posterUrl: String?,
    val screenshotUrls: List<String>
)

data class ImageIds(
    val thumbnailId: Int?,
    val posterId: Int?,
    val screenshotIds: List<Int>
)