package com.gitlab.ykrasik.gamedex.test

import com.gitlab.ykrasik.gamedex.util.getResourceAsByteArray
import javafx.scene.image.Image
import java.io.ByteArrayInputStream

/**
 * User: ykrasik
 * Date: 19/03/2017
 * Time: 13:51
 */
object TestImages {
    private fun loadImage(name: String) = getResourceAsByteArray(name)

    private val images = (0..13).map { i ->
        try {
            loadImage("game$i.jpg")
        } catch (e: Exception) {
            loadImage("game$i.png")
        }
    }

    fun randomImageBytes(): ByteArray = images.randomElement()
    fun randomImage(): Image = Image(ByteArrayInputStream(randomImageBytes()))
}