package com.github.ykrasik.gamedex.common.util

import com.google.common.io.Resources
import java.awt.Desktop
import java.io.FileNotFoundException
import java.net.URI
import java.net.URL

/**
 * User: ykrasik
 * Date: 02/10/2016
 * Time: 22:45
 */

fun String.fetchUrlData(): ByteArray? {
    val url = URL(this)
    return try {
        Resources.toByteArray(url)
    } catch (e: FileNotFoundException) {
        null
    }
}

fun browseToUrl(url: String) {
    Desktop.getDesktop().browse(URI(url))
}