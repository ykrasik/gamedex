package com.github.ykrasik.gamedex.datamodel.provider

import com.github.ykrasik.gamedex.datamodel.ImageData
import org.joda.time.LocalDate

/**
 * User: ykrasik
 * Date: 27/05/2016
 * Time: 23:16
 */
// FIXME: Something needs to be done about this class.
data class GameData(
    val name: String,
    val description: String?,
    val releaseDate: LocalDate?,

    val criticScore: Double?,
    val userScore: Double?,

    val thumbnail: ImageData?,
    val poster: ImageData?,

    val genres: List<String>,

    val metacriticUrl: String,
    val giantBombUrl: String?
) {
}