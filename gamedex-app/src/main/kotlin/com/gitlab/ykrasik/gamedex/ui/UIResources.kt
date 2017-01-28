package com.gitlab.ykrasik.gamedex.ui

import javafx.scene.image.Image

/**
 * User: ykrasik
 * Date: 02/01/2017
 * Time: 20:37
 */
object UIResources {
    private val basePath = "/com/gitlab/ykrasik/gamedex/core/ui/"

    object Images {
        val notAvailable = Image(javaClass.getResourceAsStream(basePath + "no-image-available.png"))
        val loading = Image(javaClass.getResourceAsStream(basePath + "spinner.gif"))
    }
}