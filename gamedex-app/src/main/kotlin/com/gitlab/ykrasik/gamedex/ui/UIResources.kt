package com.gitlab.ykrasik.gamedex.ui

import javafx.scene.image.Image

/**
 * User: ykrasik
 * Date: 02/01/2017
 * Time: 20:37
 */
object UIResources {
    object Images {
        // TODO: Only used by ImageLoader, consider moving these resources there.
        val notAvailable = Image(javaClass.getResourceAsStream("no-image-available.png"))
        val loading = Image(javaClass.getResourceAsStream("spinner.gif"))
    }
}