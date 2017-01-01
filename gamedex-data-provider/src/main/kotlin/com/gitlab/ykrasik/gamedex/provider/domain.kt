package com.gitlab.ykrasik.gamedex.provider

import com.github.ykrasik.gamedex.datamodel.DataProviderType
import org.joda.time.LocalDate

/**
 * User: ykrasik
 * Date: 30/12/2016
 * Time: 18:27
 */
data class SearchResult(
    val detailUrl: String,
    val name: String,
    val releaseDate: LocalDate?,
    val score: Double?,
    val thumbnailUrl: String?
)

data class ProviderGameData(
    val type: DataProviderType,
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