package com.gitlab.ykrasik.gamedex.provider.giantbomb.jackson

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.gitlab.ykrasik.gamedex.provider.DataProviderException
import com.gitlab.ykrasik.gamedex.provider.SearchResult
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
    fun assertOk() {
        if (statusCode != GiantBombStatus.ok) {
            throw DataProviderException("Invalid statusCode: $statusCode")
        }
    }
}

data class GiantBombSearchResult(
    val apiDetailUrl: String,
    val name: String,
    @JsonDeserialize(using = GiantBombJacksonDateDeserializer::class)
    val originalReleaseDate: LocalDate?,
    val image: GiantBombSearchImage
) {
    fun toSearchResult() = SearchResult(
        detailUrl = this.apiDetailUrl,
        name = this.name,
        releaseDate = this.originalReleaseDate,
        score = null,
        thumbnailUrl = this.image.thumbUrl
    )
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class GiantBombSearchImage(
    val thumbUrl: String
)