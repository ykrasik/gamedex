package com.gitlab.ykrasik.gamedex.provider

import org.joda.time.LocalDate

/**
 * User: ykrasik
 * Date: 29/05/2016
 * Time: 18:28
 */
data class SearchResult(
    val detailUrl: String,
    val name: String,
    val releaseDate: LocalDate?,
    val score: Double?
)