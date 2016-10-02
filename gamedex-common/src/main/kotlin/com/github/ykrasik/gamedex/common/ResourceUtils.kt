package com.github.ykrasik.gamedex.common

import com.google.common.io.Resources

/**
 * User: ykrasik
 * Date: 01/10/2016
 * Time: 23:13
 */

fun Any.getResourceAsByteArray(path: String): ByteArray = Resources.toByteArray(Resources.getResource(path))