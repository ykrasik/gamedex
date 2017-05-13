package com.gitlab.ykrasik.gamedex.provider.igdb

import com.gitlab.ykrasik.gamedex.*
import com.gitlab.ykrasik.gamedex.util.getResourceAsByteArray
import com.gitlab.ykrasik.gamedex.util.logger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 29/01/2017
 * Time: 11:14
 */
@Singleton
class IgdbProvider @Inject constructor(private val config: IgdbConfig, private val client: IgdbClient) : GameProvider {
    private val log = logger()

    override fun search(name: String, platform: Platform): List<ProviderSearchResult> {
        log.debug("[$platform] Searching: '$name'...")
        val searchResults = client.search(name, platform)
        val results = searchResults.toProviderSearchResults(name, platform)
        log.debug("[$platform] Searching: '$name': ${results.size} results.")
        results.forEach { log.trace("[$platform] $it") }
        return results
    }

    override fun fetch(apiUrl: String, platform: Platform): ProviderData {
        log.debug("[$platform] Fetching: $apiUrl...")
        val fetchResult = client.fetch(apiUrl)
        val gameData = fetchResult.toProviderData(apiUrl, platform)
        log.debug("[$platform] Result: $gameData.")
        return gameData
    }

    private fun List<IgdbClient.SearchResult>.toProviderSearchResults(name: String, platform: Platform): List<ProviderSearchResult> {
        // IGBD search sucks. It returns way more results then it should.
        // Since I couldn't figure out how to make it not return irrelevant results, I had to filter results myself.
        val searchWords = name.split("[^a-zA-Z\\d']".toRegex())
        val filteredResults = this.asSequence().filter { (_, name) ->
            searchWords.all { word ->
                name.contains(word, ignoreCase = true)
            }
        }
        return filteredResults.map { it.toSearchResult(platform) }.toList()
    }

    private fun IgdbClient.SearchResult.toSearchResult(platform: Platform) = ProviderSearchResult(
        apiUrl = "${config.endpoint}/$id",
        name = name,
        releaseDate = releaseDates?.findReleaseDate(platform),
        score = aggregatedRating,
        thumbnailUrl = cover?.cloudinaryId?.toImageUrl(thumbnailImageType)
    )

    private fun IgdbClient.DetailsResult.toProviderData(apiUrl: String, platform: Platform) = ProviderData(
        header = ProviderHeader(
            type = GameProviderType.Igdb,
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
            thumbnailUrl = this.cover?.cloudinaryId?.toThumbnailUrl(),
            posterUrl = this.cover?.cloudinaryId?.toPosterUrl(),
            screenshotUrls = this.screenshots?.mapNotNull { it.cloudinaryId?.toScreenshotUrl() } ?: emptyList()
        )
    )

    private fun List<IgdbClient.ReleaseDate>.findReleaseDate(platform: Platform): String? {
        // IGDB returns all release dates for all platforms, not just the one we searched for.
        val releaseDate = this.find { it.platform == platform.id } ?: return null
        return releaseDate.human
    }

    private fun String.toThumbnailUrl() = toImageUrl(thumbnailImageType)
    private fun String.toPosterUrl() = toImageUrl(posterImageType)
    private fun String.toScreenshotUrl() = toImageUrl(screenshotImageType)
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

    private val Platform.id: Int get() = config.getPlatformId(this)
    private val Int.genreName: String get() = config.getGenreName(this)

    override val name = "IGDB"
    override val type = GameProviderType.Igdb
    override val logo = IgdbProvider.logo

    companion object {
        val logo = getResourceAsByteArray("igdb.png")

        private val thumbnailImageType = IgdbImageType.thumb_2x
        private val posterImageType = IgdbImageType.screenshot_huge
        private val screenshotImageType = IgdbImageType.screenshot_huge
    }
}