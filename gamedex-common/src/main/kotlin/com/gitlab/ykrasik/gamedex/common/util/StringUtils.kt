package com.gitlab.ykrasik.gamedex.common.util

/**
 * User: ykrasik
 * Date: 02/10/2016
 * Time: 20:23
 */

// FIXME: Doesn't belong here.
private val NOT_AVAILABLE = "NA"

fun <T> T?.toString(default: String): String = this?.toString() ?: default

// FIXME: Doesn't belong here
fun <T> T?.toStringOrUnavailable(): String = toString(NOT_AVAILABLE)

// FIXME: Doesn't belong here.
val String.isUnavailable: Boolean get() = this == NOT_AVAILABLE

fun String.collapseSpaces(): String = "\\s+".toRegex().replace(this, " ")

fun String.emptyToNull(): String? = if (isNullOrEmpty()) null else this

fun String.containsIgnoreCase(str: String): Boolean = this.toLowerCase().contains(str.toLowerCase())