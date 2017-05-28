package com.gitlab.ykrasik.gamedex.provider.igdb

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.gitlab.ykrasik.gamedex.Platform
import com.gitlab.ykrasik.gamedex.util.listFromJson
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 16/04/2017
 * Time: 15:13
 */
@Singleton
open class IgdbClient @Inject constructor(private val config: IgdbConfig) {
    open fun search(name: String, platform: Platform): List<SearchResult> {
        val response = getRequest("${config.endpoint}/",
            "search" to name,
            "filter[release_dates.platform][eq]" to platform.id.toString(),
            "limit" to config.maxSearchResults.toString(),
            "fields" to searchFieldsStr
        )
        return response.listFromJson()
    }

    open fun fetch(url: String): DetailsResult {
        val response = getRequest(url,
            "fields" to fetchDetailsFieldsStr
        )

        // IGDB returns a list, even though we're fetching by id :/
        return response.listFromJson<DetailsResult> { parseError(it) }.first()
    }

    private fun getRequest(path: String, vararg parameters: Pair<String, String>) = khttp.get(path,
        params = parameters.toMap(),
        headers = mapOf(
            "Accept" to "application/json",
            "X-Mashape-Key" to config.apiKey
        )
    )

    private fun parseError(raw: String): String {
        val errors: List<Error> = raw.listFromJson()
        return errors.first().error.first()
    }

    private val Platform.id: Int get() = config.getPlatformId(this)

    private companion object {
        val searchFields = listOf(
            "name",
            "aggregated_rating",
            "aggregated_rating_count",
            "release_dates.category",
            "release_dates.human",
            "release_dates.platform",
            "cover.cloudinary_id"
        )
        val searchFieldsStr = searchFields.joinToString(",")

        val fetchDetailsFields = searchFields + listOf(
            "url",
            "summary",
            "rating",
            "rating_count",
            "screenshots.cloudinary_id",
            "genres"
        )
        val fetchDetailsFieldsStr = fetchDetailsFields.joinToString(",")
    }
    
    data class SearchResult(
        val id: Int,
        val name: String,
        val aggregatedRating: Double?,
        val aggregatedRatingCount: Int?,
        val releaseDates: List<ReleaseDate>?,
        val cover: Image?
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class ReleaseDate(
        val platform: Int,
        val category: Int,
        val human: String
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class DetailsResult(
        val url: String,
        val name: String,
        val summary: String?,
        val releaseDates: List<ReleaseDate>?,
        val aggregatedRating: Double?,
        val aggregatedRatingCount: Int?,
        val rating: Double?,
        val ratingCount: Int?,
        val cover: Image?,
        val screenshots: List<Image>?,
        val genres: List<Int>?
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Image(
        val cloudinaryId: String?
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Error(
        val error: List<String>
    )
}