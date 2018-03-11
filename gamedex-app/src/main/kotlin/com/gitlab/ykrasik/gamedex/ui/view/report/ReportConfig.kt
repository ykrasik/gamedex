package com.gitlab.ykrasik.gamedex.ui.view.report

import com.gitlab.ykrasik.gamedex.core.Filter

/**
 * User: ykrasik
 * Date: 28/01/2018
 * Time: 09:23
 */
data class ReportConfig(
    val name: String,
    val filter: Filter,
    val excludedGames: List<Int>
)