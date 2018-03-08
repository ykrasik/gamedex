package com.gitlab.ykrasik.gamedex.provider.giantbomb

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming
import com.gitlab.ykrasik.gamedex.Platform
import com.gitlab.ykrasik.gamedex.util.*
import org.joda.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 19/04/2017
 * Time: 09:33
 */
// TODO: GiantBomb can return a number of user reviews
@Singleton
open class GiantBombClient @Inject constructor(private val config: GiantBombConfig) {
    private val log = logger()

    open fun search(name: String, platform: Platform, account: GiantBombUserAccount): SearchResponse {
        val response = getRequest(config.endpoint, account,
            "filter" to "name:$name,platforms:${platform.id}",
            "field_list" to searchFieldsStr
        )
        log.trace { "[$platform] Search '$name': ${response.text}" }
        return response.fromJson()
    }

    open fun fetch(apiUrl: String, account: GiantBombUserAccount): DetailsResponse {
        val response = getRequest(apiUrl, account, "field_list" to fetchDetailsFieldsStr)
        log.trace { "Fetch '$apiUrl': ${response.text}" }
        return response.fromJson()
    }

    private val Platform.id: Int get() = config.getPlatformId(this)

    private fun getRequest(path: String, account: GiantBombUserAccount, vararg parameters: Pair<String, String>) = khttp.get(path,
        params = mapOf("api_key" to account.apiKey, "format" to "json", *parameters)
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
            "genres",
            "images"
        )
        val fetchDetailsFieldsStr = fetchDetailsFields.joinToString(",")
    }

    @JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy::class)
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class SearchResponse(
        val statusCode: Status,
        val results: List<SearchResult>
    )

    @JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy::class)
    data class SearchResult(
        val apiDetailUrl: String,
        val name: String,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")   // TODO: Can I just use a string and split by space?
        val originalReleaseDate: LocalDate?,
        val image: Image?
    )

    @JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy::class)
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class DetailsResponse(
        val statusCode: Status,

        // When result is found - GiantBomb returns a Json object. When result is not found, GiantBomb returns an empty Json array []. Annoying.
        @JsonFormat(with = [(JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)])
        val results: List<DetailsResult>
    )

    @JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy::class)
    data class DetailsResult(
        val siteDetailUrl: String,
        val name: String,
        val deck: String?,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        val originalReleaseDate: LocalDate?,
        val image: Image?,
        val images: List<Image>,
        val genres: List<Genre>?
    )

    @JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy::class)
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Genre(
        val name: String
    )

    @JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy::class)
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Image(
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