package com.gitlab.ykrasik.gamedex.provider.giantbomb

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.gitlab.ykrasik.gamedex.common.exception.GameDexException
import com.gitlab.ykrasik.gamedex.common.util.*
import com.gitlab.ykrasik.gamedex.datamodel.*
import com.gitlab.ykrasik.gamedex.provider.DataProvider
import com.gitlab.ykrasik.gamedex.provider.DataProviderInfo
import com.gitlab.ykrasik.gamedex.provider.ProviderFetchResult
import com.gitlab.ykrasik.gamedex.provider.ProviderSearchResult
import org.joda.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 29/05/2016
 * Time: 10:53
 */
@Singleton
class GiantBombDataProvider @Inject constructor(private val config: GiantBombConfig) : DataProvider {
    private val log by logger()

    private val searchFields = listOf(
        "api_detail_url",
        "name",
        "original_release_date",
        "image"
    ).joinToString(",")

    private val fetchDetailsFields = listOf(
        "site_detail_url",
        "deck",
        "image",
        "genres"
    ).joinToString(",")

    override fun search(name: String, platform: GamePlatform): List<ProviderSearchResult> {
        log.info { "Searching: name='$name', platform=$platform..." }
        val response = doSearch(name, platform)
        log.debug { "Response: $response" }

        assertOk(response.statusCode)

        val results = response.results.map { it.toSearchResult() }
        log.info { "Done(${results.size}): $results." }
        return results
    }

    private fun doSearch(name: String, platform: GamePlatform): GiantBomb.SearchResponse {
        val response = getRequest(config.endpoint,
            "filter" to "name:$name,platforms:${platform.id}",
            "field_list" to searchFields
        )
        return response.fromJson()
    }

    private fun GiantBomb.SearchResult.toSearchResult() = ProviderSearchResult(
        name = name,
        releaseDate = originalReleaseDate,
        score = null,
        thumbnailUrl = image?.thumbUrl,
        apiUrl = apiDetailUrl
    )

    override fun fetch(searchResult: ProviderSearchResult): ProviderFetchResult {
        log.info { "Fetching: $searchResult..." }
        val response = doFetch(searchResult.apiUrl)
        log.debug { "Response: $response" }

        assertOk(response.statusCode)

        // When result is found - GiantBomb returns a Json object.
        // When result is not found, GiantBomb returns an empty Json array [].
        // So 'results' can contain at most a single value.
        val gameData = response.results.first().toFetchResult(searchResult)
        log.info { "Done: $gameData." }
        return gameData
    }

    private fun doFetch(detailUrl: String): GiantBomb.DetailsResponse {
        val response = getRequest(detailUrl, "field_list" to fetchDetailsFields)
        return response.fromJson()
    }

    private fun GiantBomb.DetailsResult.toFetchResult(searchResult: ProviderSearchResult) = ProviderFetchResult(
        providerData = ProviderData(
            type = DataProviderType.GiantBomb,
            apiUrl = searchResult.apiUrl,
            url = siteDetailUrl
        ),
        gameData = GameData(
            name = searchResult.name,
            description = deck,
            releaseDate = searchResult.releaseDate,
            criticScore = null,
            userScore = null,
            genres = genres?.map { it.name } ?: emptyList()
        ),
        imageUrls = ImageUrls(
            thumbnailUrl = image?.thumbUrl,
            posterUrl = image?.superUrl,
            screenshotUrls = emptyList()
            // TODO: Support screenshots
        )
    )

    private val GamePlatform.id: Int get() = config.getPlatformId(this)

    private fun assertOk(status: GiantBomb.Status) {
        if (status != GiantBomb.Status.ok) {
            throw GameDexException("Invalid statusCode: $status")
        }
    }

    private fun getRequest(path: String, vararg parameters: Pair<String, String>) = khttp.get(path,
        params = mapOf("api_key" to config.apiKey, "format" to "json", *parameters)
    )

    override val info = GiantBomb.info
}

object GiantBomb {
    val info = DataProviderInfo(
        name = "GiantBomb",
        type = DataProviderType.GiantBomb,
        logo = getResourceAsByteArray("/com/gitlab/ykrasik/gamedex/provider/giantbomb/giantbomb.png").toImage()
    )

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
        val deck: String?,
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

        override fun toString() = "$name($key)"

        companion object {
            private val values = EnumIdConverter(Status::class.java)

            @JsonCreator
            @JvmStatic
            operator fun invoke(code: Int): Status = values[code]
        }
    }
}