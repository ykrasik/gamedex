package com.gitlab.ykrasik.gamedex.util

import com.google.common.io.Resources
import java.awt.Desktop
import java.net.URI

/**
 * User: ykrasik
 * Date: 10/02/2017
 * Time: 09:02
 */
typealias Extractor<T, R> = T.() -> R
typealias Modifier<T, R> = T.(R) -> T
typealias NestedModifier<T, R> = T.(R.() -> R) -> T

fun Any.getResourceAsByteArray(path: String): ByteArray = Resources.toByteArray(Resources.getResource(javaClass, path))

val Int.kb: Int get() = this * 1024

fun String.browseToUrl() {
    Desktop.getDesktop().browse(URI(this))
}