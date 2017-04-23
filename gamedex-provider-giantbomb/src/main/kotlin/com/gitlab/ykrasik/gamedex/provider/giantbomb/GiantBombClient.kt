package com.gitlab.ykrasik.gamedex.provider.giantbomb

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.gitlab.ykrasik.gamedex.GamePlatform
import com.gitlab.ykrasik.gamedex.util.EnumIdConverter
import com.gitlab.ykrasik.gamedex.util.IdentifiableEnum
import com.gitlab.ykrasik.gamedex.util.fromJson
import org.joda.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 19/04/2017
 * Time: 09:33
 */
@Singleton
open class GiantBombClient @Inject constructor(private val config: GiantBombConfig) {
    open fun search(name: String, platform: GamePlatform): SearchResponse {
        val response = getRequest(config.endpoint,
            "filter" to "name:$name,platforms:${platform.id}",
            "field_list" to searchFieldsStr
        )
        return response.fromJson()
    }

    open fun fetch(apiUrl: String): DetailsResponse {
        val response = getRequest(apiUrl, "field_list" to fetchDetailsFieldsStr)
        return response.fromJson()
    }

    private val GamePlatform.id: Int get() = config.getPlatformId(this)

    private fun getRequest(path: String, vararg parameters: Pair<String, String>) = khttp.get(path,
        params = mapOf("api_key" to config.apiKey, "format" to "json", *parameters)
    )

    private companion object {
        val searchFields = listOf(
            "api_detail_url",
            "name",
            "original_release_date",
            "image"
        )
        val searchFieldsStr = searchFields.joinToString(",")

        val fetchDetailsFields = searchFields - "api_detail_url" + listOf(
            "site_detail_url",
            "deck",
            "genres"
        )
        val fetchDetailsFieldsStr = fetchDetailsFields.joinToString(",")
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class SearchResponse(
        val statusCode: Status,
        val results: List<SearchResult>
    )

    data class SearchResult(
        val apiDetailUrl: String,
        val name: String,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        val originalReleaseDate: LocalDate?,
        val image: SearchImage?
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class SearchImage(
        val thumbUrl: String
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class DetailsResponse(
        val statusCode: Status,

        // When result is found - GiantBomb returns a Json object. When result is not found, GiantBomb returns an empty Json array []. Annoying.
        @JsonFormat(with = arrayOf(JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY))
        val results: List<DetailsResult>
    )

    data class DetailsResult(
        val siteDetailUrl: String,
        val name: String,
        val deck: String?,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        val originalReleaseDate: LocalDate?,
        val image: DetailsImage?,
        val genres: List<Genre>?
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Genre(
        val name: String
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class DetailsImage(
        val thumbUrl: String,
        val superUrl: String
    )

    enum class Status constructor(override val key: Int) : IdentifiableEnum<Int> {
        ok(1),
        invalidApiKey(100),
        notFound(101),
        badFormat(102),
        jsonPNoCallback(103),
        filterError(104),
        videoOnlyForSubscribers(105);

        // TODO: Try adding a public getter for key with @JsonValue, will not need IdentifiableEnum then.

        override fun toString() = "$name($key)"

        companion object {
            private val values = EnumIdConverter(Status::class.java)

            @JsonCreator
            @JvmStatic
            operator fun invoke(code: Int): Status = values[code]
        }
    }
}