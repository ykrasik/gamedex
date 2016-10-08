package com.gitlab.ykrasik.gamedex.provider

import org.joda.time.LocalDate

/**
 * User: ykrasik
 * Date: 29/05/2016
 * Time: 18:24
 */
data class ProviderGameData(
    val detailUrl: String,
    val name: String,
    val description: String?,
    val releaseDate: LocalDate?,
    val criticScore: Double?,
    val userScore: Double?,
    val thumbnailUrl: String?,
    val posterUrl: String?,
    val genres: List<String>
)