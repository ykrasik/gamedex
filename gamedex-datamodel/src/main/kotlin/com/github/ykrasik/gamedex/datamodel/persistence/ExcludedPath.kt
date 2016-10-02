package com.github.ykrasik.gamedex.datamodel.persistence

import java.nio.file.Path

/**
 * User: ykrasik
 * Date: 25/05/2016
 * Time: 12:08
 */
data class ExcludedPath(
    val id: Int,
    val path: Path
) {
}