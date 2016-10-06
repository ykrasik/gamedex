package com.gitlab.ykrasik.gamedex.provider.giantbomb.client

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.gitlab.ykrasik.gamedex.provider.giantbomb.jackson.GiantBombJacksonDateDeserializer
import org.joda.time.LocalDate

/**
 * User: ykrasik
 * Date: 01/10/2016
 * Time: 10:54
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class GiantBombSearchResponse(
    val statusCode: GiantBombStatus,
    val results: List<GiantBombSearchResult>
) {
    fun isOk() = statusCode == GiantBombStatus.ok
}

data class GiantBombSearchResult(
    val apiDetailUrl: String,
    val name: String,
    @JsonDeserialize(using = GiantBombJacksonDateDeserializer::class)
    val originalReleaseDate: LocalDate?,
    val image: GiantBombSearchImage
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GiantBombSearchImage(
    val thumbUrl: String
)