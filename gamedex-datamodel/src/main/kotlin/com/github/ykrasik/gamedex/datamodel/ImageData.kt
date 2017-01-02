package com.github.ykrasik.gamedex.datamodel

import com.github.ykrasik.gamedex.common.getResourceAsByteArray

/**
 * User: ykrasik
 * Date: 28/05/2016
 * Time: 14:02
 */
class ImageData(
    val rawData: ByteArray?,
    val url: String?
) {
    override fun toString() = url ?: "Empty"

    companion object {
        val empty = ImageData(rawData = null, url = null)

        fun fromFile(fileName: String): ImageData = ImageData(
            rawData = getResourceAsByteArray(fileName),
            url = fileName
        )
    }
}