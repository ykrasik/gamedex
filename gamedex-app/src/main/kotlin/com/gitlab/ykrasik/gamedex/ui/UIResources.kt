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

        val confirm = Image(javaClass.getResourceAsStream("confirm.png"))
        val information = Image(javaClass.getResourceAsStream("information.png"))
        val warning = Image(javaClass.getResourceAsStream("warning.png"))
        val error = Image(javaClass.getResourceAsStream("error.png"))
    }
}