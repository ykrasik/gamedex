package com.gitlab.ykrasik.gamedex.test

import com.gitlab.ykrasik.gamedex.util.ClassPathScanner
import javafx.scene.image.Image
import java.io.ByteArrayInputStream

/**
 * User: ykrasik
 * Date: 19/03/2017
 * Time: 13:51
 *
 * These images were all taken from igdb.com
 */
object TestImages {
    private val images = ClassPathScanner.scanResources("com.gitlab.ykrasik.gamedex.test") { it.endsWith(".jpg") }

    fun randomImageBytes(): ByteArray = images.randomElement().readBytes()
    fun randomImage(): Image = Image(ByteArrayInputStream(randomImageBytes()))
}