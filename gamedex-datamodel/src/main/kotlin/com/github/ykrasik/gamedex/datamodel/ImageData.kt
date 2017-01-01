package com.github.ykrasik.gamedex.datamodel

import com.github.ykrasik.gamedex.common.getResourceAsByteArray

/**
 * User: ykrasik
 * Date: 28/05/2016
 * Time: 14:02
 */
class ImageData(
    val rawData: ByteArray
) {
//    val image: Image by lazy { Image(ByteArrayInputStream(rawData)) }

    companion object {
        fun fromFile(fileName: String): ImageData = ImageData(getResourceAsByteArray(fileName))
//        fun fromUrl(url: URL): ImageData = ImageData(Resources.toByteArray(url))
//        fun fromUrl(url: String): ImageData = fromUrl(URL(url))
    }
}