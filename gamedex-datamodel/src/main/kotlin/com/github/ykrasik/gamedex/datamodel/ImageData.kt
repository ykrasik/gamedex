package com.github.ykrasik.gamedex.datamodel

import com.github.ykrasik.gamedex.common.getResourceAsByteArray
import com.github.ykrasik.gamedex.common.toImage
import javafx.scene.image.Image

/**
 * User: ykrasik
 * Date: 28/05/2016
 * Time: 14:02
 */
// FIXME: This class is probably not needed.
class ImageData(
    // TODO: rawData is not necessary, data should be lazily fetched when it is first accessed and streamed afterwards.
    val bytes: ByteArray?,
    val url: String?
) {

    fun toImage(): Image = bytes!!.toImage()

    override fun toString() = url ?: "Empty"

    companion object {
        val empty = ImageData(bytes = null, url = null)

        fun fromFile(fileName: String): ImageData = ImageData(
            bytes = getResourceAsByteArray(fileName),
            url = fileName
        )
    }
}