package com.gitlab.ykrasik.gamedex.provider.giantbomb

import com.gitlab.ykrasik.gamedex.common.datamodel.*
import com.gitlab.ykrasik.gamedex.common.exception.GameDexException
import com.gitlab.ykrasik.gamedex.common.util.fromJson
import com.gitlab.ykrasik.gamedex.common.util.getResourceAsByteArray
import com.gitlab.ykrasik.gamedex.common.util.logger
import com.gitlab.ykrasik.gamedex.common.util.toImage
import com.gitlab.ykrasik.gamedex.provider.DataProvider
import com.gitlab.ykrasik.gamedex.provider.DataProviderInfo
import com.gitlab.ykrasik.gamedex.provider.ProviderFetchResult
import com.gitlab.ykrasik.gamedex.provider.ProviderSearchResult
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

    private val endpoint = "http://www.giantbomb.com/api/games"

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

    private fun doSearch(name: String, platform: GamePlatform): GiantBombSearchResponse {
        val response = getRequest(endpoint,
            "filter" to "name:$name,platforms:${platform.id}",
            "field_list" to searchFields
        )
        return response.fromJson()
    }

    private fun GiantBombSearchResult.toSearchResult() = ProviderSearchResult(
        name = name,
        releaseDate = originalReleaseDate,
        score = null,
        thumbnailUrl = image.thumbUrl,
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

    private fun doFetch(detailUrl: String): GiantBombDetailsResponse {
        val response = getRequest(detailUrl, "field_list" to fetchDetailsFields)
        return response.fromJson()
    }

    private fun GiantBombDetailsResult.toFetchResult(searchResult: ProviderSearchResult) = ProviderFetchResult(
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
            genres = genres.map { it.name }
        ),
        imageUrls = ImageUrls(
            thumbnailUrl = image.thumbUrl,
            posterUrl = image.superUrl,
            screenshotUrls = emptyList()
            // TODO: Support screenshots
        )
    )

    private val GamePlatform.id: Int get() = config.getPlatformId(this)

    private fun assertOk(status: GiantBombStatus) {
        if (status != GiantBombStatus.ok) {
            throw GameDexException("Invalid statusCode: $status")
        }
    }

    private fun getRequest(path: String, vararg parameters: Pair<String, String>) = khttp.get(path,
        params = mapOf("api_key" to config.applicationKey, "format" to "json", *parameters)
    )

    override val info = GiantBombDataProvider.info

    companion object {
        val info = DataProviderInfo(
            name = "GiantBomb",
            type = DataProviderType.GiantBomb,
            logo = getResourceAsByteArray("/com/gitlab/ykrasik/gamedex/provider/giantbomb/giantbomb.png").toImage()
        )
    }
}