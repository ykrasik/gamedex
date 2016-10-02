package com.github.ykrasik.gamedex.datamodel

import javafx.scene.image.Image
import java.io.ByteArrayInputStream

/**
 * User: ykrasik
 * Date: 28/05/2016
 * Time: 14:02
 */
class ImageData(
    val rawData: ByteArray,
    val image: Image    // FIXME: Doesn't belong here.
) {

    companion object {
        operator fun invoke(rawData: ByteArray): ImageData = ImageData(rawData, Image(ByteArrayInputStream(rawData)))
    }
}