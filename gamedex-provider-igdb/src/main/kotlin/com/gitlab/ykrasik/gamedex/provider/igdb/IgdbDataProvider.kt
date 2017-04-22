package com.gitlab.ykrasik.gamedex.provider.igdb

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.gitlab.ykrasik.gamedex.*
import com.gitlab.ykrasik.gamedex.util.containsIgnoreCase
import com.gitlab.ykrasik.gamedex.util.getResourceAsByteArray
import com.gitlab.ykrasik.gamedex.util.logger
import com.gitlab.ykrasik.gamedex.util.toImage
import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 29/01/2017
 * Time: 11:14
 */
@Singleton
class IgdbDataProvider @Inject constructor(private val config: IgdbConfig, private val client: IgdbClient) : DataProvider {
    private val log by logger()

    override fun search(name: String, platform: GamePlatform): List<ProviderSearchResult> {
        log.info { "[$platform] Searching: name='$name'..." }
        val searchResults = client.search(name, platform)
        log.debug { "[$platform] Results: $searchResults" }

        val results = searchResults.toProviderSearchResults(name, platform)
        log.info { "[$platform] ${results.size} Search results: $results." }
        return results
    }

    override fun fetch(apiUrl: String, platform: GamePlatform): ProviderFetchResult {
        log.info { "[$platform] Fetching: $apiUrl..." }
        val fetchResult = client.fetch(apiUrl)
        log.debug { "[$platform] Response: $fetchResult" }

        val gameData = fetchResult.toProviderFetchResult(apiUrl, platform)
        log.info { "[$platform] Result: $gameData." }
        return gameData
    }

    private fun List<Igdb.SearchResult>.toProviderSearchResults(name: String, platform: GamePlatform): List<ProviderSearchResult> {
        // IGBD search sucks. It returns way more results then it should.
        // Since I couldn't figure out how to make it not return irrelevant results, I had to filter results myself.
        val searchWords = name.split("[^a-zA-Z\\d']".toRegex())
        val filteredResults = this.asSequence().filter { (_, name) ->
            searchWords.all { word ->
                name.containsIgnoreCase(word)
            }
        }
        return filteredResults.map { it.toSearchResult(platform) }.toList()
    }

    private fun Igdb.SearchResult.toSearchResult(platform: GamePlatform) = ProviderSearchResult(
        apiUrl = "${config.endpoint}$id",
        name = name,
        releaseDate = releaseDates?.findReleaseDate(platform),
        score = aggregatedRating,
        thumbnailUrl = cover?.cloudinaryId?.toImageUrl(thumbnailImageType)
    )

    private fun Igdb.DetailsResult.toProviderFetchResult(apiUrl: String, platform: GamePlatform) = ProviderFetchResult(
        providerData = ProviderData(
            type = DataProviderType.Igdb,
            apiUrl = apiUrl,
            siteUrl = this.url
        ),
        gameData = GameData(
            name = this.name,
            description = this.summary,
            releaseDate = this.releaseDates?.findReleaseDate(platform),
            criticScore = this.aggregatedRating,
            userScore = this.rating,
            genres = this.genres?.map { it.genreName } ?: emptyList()
        ),
        imageUrls = ImageUrls(
            thumbnailUrl = this.cover?.cloudinaryId?.toImageUrl(thumbnailImageType),
            posterUrl = this.cover?.cloudinaryId?.toImageUrl(posterImageType),
            screenshotUrls = emptyList()    // TODO: Support screenshots
        )
    )

    private fun List<Igdb.ReleaseDate>.findReleaseDate(platform: GamePlatform): LocalDate? {
        // IGDB returns all release dates for all platforms, not just the one we searched for.
        val releaseDate = this.find { it.platform == platform.id } ?: return null
        return releaseDate.toLocalDate()
    }

    private fun String.toImageUrl(type: IgdbImageType) = "${config.baseImageUrl}/t_$type/$this.png"

    private enum class IgdbImageType {
        micro, // 35 x 35
        micro_2x, // 70 x 70
        thumb, // 90 x 90
        thumb_2x, // 180 x 180
        logo_med, // 284 x 160
        logo_med_2x, // 568 x 320
        cover_small, // 90 x 128
        cover_small_2x, // 180 x 256
        cover_big, // 227 x 320
        cover_big_2x, // 454 x 640
        screenshot_med, // 569 x 320
        screenshot_med_2x, // 1138 x 640
        screenshot_big, // 889 x 500
        screenshot_big_2x, // 1778 x 1000
        screenshot_huge,    // 1280 x 720
        screenshot_huge_2x  // 2560 x 1440
    }

    private val GamePlatform.id: Int get() = config.getPlatformId(this)
    private val Int.genreName: String get() = config.getGenreName(this)

    override val info = IgdbDataProvider.info

    companion object {
        val info = DataProviderInfo(
            name = "IGDB",
            type = DataProviderType.Igdb,
            logo = getResourceAsByteArray("igdb.png").toImage()
        )

        private val thumbnailImageType = IgdbImageType.thumb_2x
        private val posterImageType = IgdbImageType.screenshot_huge
    }
}

object Igdb {
    data class SearchResult(
        val id: Int,
        val name: String,
        val aggregatedRating: Double?,
        val releaseDates: List<ReleaseDate>?,
        val cover: Image?
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class ReleaseDate(
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
    data class DetailsResult(
        val url: String,
        val name: String,
        val summary: String?,
        val releaseDates: List<ReleaseDate>?,
        val aggregatedRating: Double?,
        val rating: Double?,
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