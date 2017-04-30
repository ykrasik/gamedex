package com.gitlab.ykrasik.gamedex.util

/**
 * User: ykrasik
 * Date: 02/10/2016
 * Time: 20:23
 */
fun Any?.toStringOr(default: String): String = this?.toString() ?: default

fun String.collapseSpaces(): String = "\\s+".toRegex().replace(this, " ")

fun String.emptyToNull(): String? = if (isNullOrEmpty()) null else this