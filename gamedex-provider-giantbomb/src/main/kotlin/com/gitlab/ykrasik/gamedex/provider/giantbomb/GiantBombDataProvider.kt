package com.gitlab.ykrasik.gamedex.provider.giantbomb

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.gitlab.ykrasik.gamedex.*
import com.gitlab.ykrasik.gamedex.util.*
import org.joda.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 29/05/2016
 * Time: 10:53
 */
@Singleton
class GiantBombDataProvider @Inject constructor(private val client: GiantBombClient) : DataProvider {
    private val log by logger()

    override fun search(name: String, platform: GamePlatform): List<ProviderSearchResult> {
        log.info { "[$platform] Searching: name='$name'..." }
        val response = client.search(name, platform)
        log.debug { "[$platform] Response: $response" }

        assertOk(response.statusCode)

        val results = response.results.map { it.toProviderSearchResult() }
        log.info { "[$platform] ${results.size} Search results: $results." }
        return results
    }

    private fun GiantBomb.SearchResult.toProviderSearchResult() = ProviderSearchResult(
        name = name,
        releaseDate = originalReleaseDate,
        score = null,
        thumbnailUrl = image?.thumbUrl,
        apiUrl = apiDetailUrl
    )

    override fun fetch(apiUrl: String, platform: GamePlatform): ProviderFetchResult {
        log.info { "[$platform] Fetching: $apiUrl..." }
        val response = client.fetch(apiUrl)
        log.debug { "[$platform] Response: $response" }

        assertOk(response.statusCode)

        // When result is found - GiantBomb returns a Json object.
        // When result is not found, GiantBomb returns an empty Json array [].
        // So 'results' can contain at most a single value.
        val gameData = response.results.first().toProviderFetchResult(apiUrl)
        log.info { "[$platform] Result: $gameData." }
        return gameData
    }

    private fun GiantBomb.DetailsResult.toProviderFetchResult(apiUrl: String) = ProviderFetchResult(
        providerData = ProviderData(
            type = DataProviderType.GiantBomb,
            apiUrl = apiUrl,
            siteUrl = this.siteDetailUrl
        ),
        gameData = GameData(
            name = this.name,
            description = this.deck,
            releaseDate = this.originalReleaseDate,
            criticScore = null,
            userScore = null,
            genres = this.genres?.map { it.name } ?: emptyList()
        ),
        imageUrls = ImageUrls(
            thumbnailUrl = this.image?.thumbUrl,
            posterUrl = this.image?.superUrl,
            screenshotUrls = emptyList()    // TODO: Support screenshots
        )
    )

    private fun assertOk(status: GiantBomb.Status) {
        if (status != GiantBomb.Status.ok) {
            throw GameDexException("Invalid statusCode: $status")
        }
    }

    override val info = GiantBombDataProvider.info

    companion object {
        val info = DataProviderInfo(
            name = "GiantBomb",
            type = DataProviderType.GiantBomb,
            logo = getResourceAsByteArray("giantbomb.png").toImage()
        )
    }
}

object GiantBomb {
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