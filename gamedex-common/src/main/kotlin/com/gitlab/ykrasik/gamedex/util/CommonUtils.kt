package com.gitlab.ykrasik.gamedex.util

import com.google.common.io.Resources
import java.awt.Desktop
import java.net.URI

/**
 * User: ykrasik
 * Date: 10/02/2017
 * Time: 09:02
 */

fun Any.getResourceAsByteArray(path: String): ByteArray = Resources.toByteArray(Resources.getResource(javaClass, path))

val Int.kb: Int get() = this * 1024

fun String.browseToUrl() {
    Desktop.getDesktop().browse(URI(this))
}