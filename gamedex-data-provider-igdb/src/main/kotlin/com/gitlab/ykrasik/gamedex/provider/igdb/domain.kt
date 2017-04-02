package com.gitlab.ykrasik.gamedex.provider.igdb

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat

/**
 * User: ykrasik
 * Date: 29/01/2017
 * Time: 11:24
 */
data class IgdbSearchResult(
    val id: Int,
    val name: String,
    val aggregatedRating: Double?,
    val releaseDates: List<IgdbReleaseDate>?,
    val cover: IgdbImage?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class IgdbReleaseDate(
    val platform: Int,
    val category: Int,
    val human: String
) {
    fun toLocalDate(): LocalDate? {
        val format = when (category) {
            0 -> DateTimeFormat.forPattern("YYYY-MMM-dd")
            1 -> DateTimeFormat.forPattern("YYYY-MMM")
            2 -> DateTimeFormat.forPattern("YYYY")
            3 -> DateTimeFormat.forPattern("YYYY-'Q1'")
            4 -> DateTimeFormat.forPattern("YYYY-'Q2'")
            5 -> DateTimeFormat.forPattern("YYYY-'Q3'")
            6 -> DateTimeFormat.forPattern("YYYY-'Q4'")
            7 -> return null
            else -> throw IllegalArgumentException("Invalid date category: $category!")
        }
        return format.parseLocalDate(human)
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class IgdbDetailsResult(
    val url: String,
    val summary: String?,
    val rating: Double?,
    val cover: IgdbImage?,
    val screenshots: List<IgdbImage>?,
    val genres: List<Int>?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class IgdbImage(
    val cloudinaryId: String?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class IgdbError(
    val error: List<String>
)