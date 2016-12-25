package com.github.ykrasik.gamedex.datamodel

import com.github.ykrasik.gamedex.common.Id
import java.nio.file.Path

/**
 * User: ykrasik
 * Date: 25/05/2016
 * Time: 12:08
 */
data class ExcludedPath(
    val id: Id<ExcludedPath>,
    val path: Path
)