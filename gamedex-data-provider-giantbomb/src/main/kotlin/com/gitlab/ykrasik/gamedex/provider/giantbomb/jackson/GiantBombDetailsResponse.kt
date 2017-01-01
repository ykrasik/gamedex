package com.gitlab.ykrasik.gamedex.provider.giantbomb.jackson

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.gitlab.ykrasik.gamedex.provider.DataProviderException
import org.joda.time.LocalDate

/**
 * User: ykrasik
 * Date: 01/10/2016
 * Time: 10:54
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class GiantBombDetailsResponse(
    val statusCode: GiantBombStatus,

    // When result is found - GiantBomb returns a Json object. When result is not found, GiantBomb returns an empty Json array []. Annoying.
    @JsonFormat(with = arrayOf(JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY))
    val results: List<GiantBombDetailsResult>
) {
    fun assertOk() {
        if (statusCode != GiantBombStatus.ok) {
            throw DataProviderException("Invalid statusCode: $statusCode")
        }
    }
}

data class GiantBombDetailsResult(
    val name: String,
    val deck: String?,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    val originalReleaseDate: LocalDate?,
    val image: GiantBombDetailsImage,
    val genres: List<GiantBombGenre>
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GiantBombGenre(
    val name: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GiantBombDetailsImage(
    val thumbUrl: String,
    val superUrl: String
)