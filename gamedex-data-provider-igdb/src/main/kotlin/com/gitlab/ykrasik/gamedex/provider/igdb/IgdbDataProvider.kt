package com.gitlab.ykrasik.gamedex.provider.igdb

import com.gitlab.ykrasik.gamedex.common.datamodel.*
import com.gitlab.ykrasik.gamedex.common.util.*
import com.gitlab.ykrasik.gamedex.provider.DataProvider
import com.gitlab.ykrasik.gamedex.provider.DataProviderInfo
import com.gitlab.ykrasik.gamedex.provider.ProviderFetchResult
import com.gitlab.ykrasik.gamedex.provider.ProviderSearchResult
import org.joda.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 29/01/2017
 * Time: 11:14
 */
@Singleton
class IgdbDataProvider @Inject constructor(private val config: IgdbConfig) : DataProvider {
    private val log by logger()

    private val searchFields = listOf(
        "name",
        "aggregated_rating",
        "release_dates.category",
        "release_dates.human",
        "release_dates.platform",
        "cover.cloudinary_id"
    ).joinToString(",")

    private val fetchDetailsFields = listOf(
        "url",
        "summary",
        "aggregated_rating",
        "rating",
        "cover.cloudinary_id",
        "screenshots.cloudinary_id",
        "genres"
    ).joinToString(",")

    override fun search(name: String, platform: GamePlatform): List<ProviderSearchResult> {
        log.info { "Searching: name='$name', platform=$platform..." }
        val response = doSearch(name, platform)
        log.debug { "Response: $response" }

        val results = toSearchResults(response, name, platform)
        log.info { "Done(${results.size}): $results." }
        return results
    }

    private fun doSearch(name: String, platform: GamePlatform): List<IgdbSearchResult> {
        val response = getRequest(config.endpoint,
            "search" to name,
            "filter[release_dates.platform][eq]" to platform.id.toString(),
            "limit" to 20.toString(),
            "fields" to searchFields
        )
        return response.listFromJson()
    }

    private fun IgdbSearchResult.toSearchResult(platform: GamePlatform) = ProviderSearchResult(
        apiUrl = "${config.endpoint}$id",
        name = name,
        releaseDate = findReleaseDate(platform),
        score = aggregatedRating,
        thumbnailUrl = cover?.cloudinaryId?.let { imageUrl(it, IgdbImageType.thumb, x2 = true) }
    )

    private fun IgdbSearchResult.findReleaseDate(platform: GamePlatform): LocalDate? {
        // IGDB returns all release dates for all platforms, not just the one we searched for.
        val releaseDates = this.releaseDates ?: return null
        val releaseDate = releaseDates.find { it.platform == platform.id } ?: return null
        return releaseDate.toLocalDate()
    }

    // TODO: Unit test this.
    private fun toSearchResults(response: List<IgdbSearchResult>, name: String, platform: GamePlatform): List<ProviderSearchResult> {
        // IGBD search sucks. It returns way more results then it should.
        // Since I couldn't figure out how to make it not return irrelevant results, I had to filter results myself.
        val searchWords = name.split("[^a-zA-Z\\d']".toRegex())
        return response.asSequence().filter { (_, name) ->
            searchWords.all { word ->
                name.containsIgnoreCase(word)
            }
        }.map { it.toSearchResult(platform) }.toList()
    }

    override fun fetch(searchResult: ProviderSearchResult): ProviderFetchResult {
        log.info { "Fetching: $searchResult..." }
        val response = doFetch(searchResult)
        log.debug { "Response: $response" }

        // When result is found - GiantBomb returns a Json object.
        // When result is not found, GiantBomb returns an empty Json array [].
        // So 'results' can contain at most a single value.
        val gameData = response.toFetchResult(searchResult)
        log.info { "Done: $gameData." }
        return gameData
    }

    private fun doFetch(searchResult: ProviderSearchResult): IgdbDetailsResult {
        val response = getRequest(searchResult.apiUrl, "fields" to fetchDetailsFields)
        // IGDB returns a list, even though we're fetching by id :/
        return response.listFromJson<IgdbDetailsResult> { parseError(it) }.first()
    }

    private fun parseError(raw: String): String {
        val errors: List<IgdbError> = raw.listFromJson()
        return errors.first().error.first()
    }

    private fun IgdbDetailsResult.toFetchResult(searchResult: ProviderSearchResult) = ProviderFetchResult(
        providerData = ProviderData(
            type = DataProviderType.Igdb,
            apiUrl = searchResult.apiUrl,
            url = url
        ),
        gameData = GameData(
            name = searchResult.name,
            description = summary,
            releaseDate = searchResult.releaseDate,
            criticScore = aggregatedRating,
            userScore = rating,
            genres = genres?.map { it.genreName } ?: emptyList()
        ),
        imageUrls = ImageUrls(
            thumbnailUrl = searchResult.thumbnailUrl,
            posterUrl = cover?.cloudinaryId?.let { imageUrl(it, IgdbImageType.screenshot_huge) },
            screenshotUrls = emptyList()
            // TODO: Support screenshots
        )
    )

    private fun getRequest(path: String, vararg parameters: Pair<String, String>) = khttp.get(path,
        params = parameters.toMap(),
        headers = mapOf(
            "Accept" to "application/json",
            "X-Mashape-Key" to config.apiKey
        )
    )

    private fun imageUrl(hash: String, type: IgdbImageType, x2: Boolean = false) =
        "${config.baseImageUrl}/t_$type${if (x2) "_2x" else ""}/$hash.png"

    private enum class IgdbImageType {
        micro, // 35 x 35
        thumb, // 90 x 90
        logo_med, // 284 x 160
        cover_small, // 90 x 128
        cover_big, // 227 x 320
        screenshot_med, // 569 x 320
        screenshot_big, // 889 x 500
        screenshot_huge     // 1280 x 720
    }

    private val GamePlatform.id: Int get() = config.getPlatformId(this)
    private val Int.genreName: String get() = config.getGenreName(this)

    override val info = IgdbDataProvider.info

    companion object {
        val info = DataProviderInfo(
            name = "IGDB",
            type = DataProviderType.GiantBomb,
            logo = getResourceAsByteArray("/com/gitlab/ykrasik/gamedex/provider/igdb/igdb.png").toImage()
        )
    }
}