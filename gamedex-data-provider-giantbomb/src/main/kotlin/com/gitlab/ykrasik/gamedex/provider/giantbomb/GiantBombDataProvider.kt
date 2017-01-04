package com.gitlab.ykrasik.gamedex.provider.giantbomb

import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.HttpException
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import com.github.ykrasik.gamedex.common.jackson.objectMapper
import com.github.ykrasik.gamedex.common.logger
import com.github.ykrasik.gamedex.datamodel.DataProviderType
import com.github.ykrasik.gamedex.datamodel.GameImageData
import com.github.ykrasik.gamedex.datamodel.GamePlatform
import com.github.ykrasik.gamedex.datamodel.ImageData
import com.gitlab.ykrasik.gamedex.provider.DataProvider
import com.gitlab.ykrasik.gamedex.provider.DataProviderInfo
import com.gitlab.ykrasik.gamedex.provider.ProviderFetchResult
import com.gitlab.ykrasik.gamedex.provider.ProviderSearchResult
import com.gitlab.ykrasik.gamedex.provider.giantbomb.jackson.GiantBombDetailsResponse
import com.gitlab.ykrasik.gamedex.provider.giantbomb.jackson.GiantBombDetailsResult
import com.gitlab.ykrasik.gamedex.provider.giantbomb.jackson.GiantBombSearchResponse
import com.gitlab.ykrasik.gamedex.provider.giantbomb.jackson.GiantBombStatus
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

    override val info = DataProviderInfo(
        name = "GiantBomb",
        type = DataProviderType.GiantBomb,
        logo = ImageData.fromFile("/com/gitlab/ykrasik/gamedex/provider/giantbomb/giantbomb.png")
    )

    private val searchFields = listOf("api_detail_url", "name", "original_release_date", "image").joinToString(",")
    private val fetchDetailsFields = listOf("name", "deck", "original_release_date", "image", "genres").joinToString(",")

    override fun search(name: String, platform: GamePlatform): List<ProviderSearchResult> {
        log.info { "Search: name='$name', platform=$platform..." }
        val response = doSearch(name, platform)
        log.debug { "Response: $response" }
        response.assertOk()

        val results = response.results.map { it.toSearchResult() }
        log.info { "Done: $results." }
        return results
    }

    private fun doSearch(name: String, platform: GamePlatform): GiantBombSearchResponse {
        val platformId = config.getPlatformId(platform)
        val (request, response, result) = getRequest("http://www.giantbomb.com/api/games",
            "filter" to "name:$name,platforms:$platformId",
            "field_list" to searchFields
        )
        return result.fromJson(GiantBombSearchResponse::class.java)
    }

    override fun fetch(searchResult: ProviderSearchResult): ProviderFetchResult {
        log.info { "Fetch: $searchResult..." }
        val detailUrl = searchResult.detailUrl
        val response = doFetch(detailUrl)
        log.debug { "Response: $response" }
        response.assertOk()

        // When result is found - GiantBomb returns a Json object.
        // When result is not found, GiantBomb returns an empty Json array [].
        // So 'results' can contain at most a single value.
        val gameData = response.results.first().toProviderGameData(detailUrl)
        log.info { "Done: $gameData." }
        return gameData
    }

    private fun doFetch(detailUrl: String): GiantBombDetailsResponse = try {
        val (request, response, result) = getRequest(detailUrl, "field_list" to fetchDetailsFields)
        result.fromJson(GiantBombDetailsResponse::class.java)
    } catch (e: FuelError) {
        e.exception.let {
            when (it) {
                is HttpException -> if (it.httpCode == 404) {
                    GiantBombDetailsResponse(statusCode = GiantBombStatus.notFound, results = emptyList())
                } else {
                    throw e
                }
                else -> throw e
            }
        }
    }

    private fun getRequest(path: String, vararg elements: Pair<String, Any?>) = path.httpGet(params(elements))
        .header("User-Agent" to "Kotlin-Fuel")
        .response()

    private fun params(elements: Array<out Pair<String, Any?>>): List<Pair<String, Any?>> = mutableListOf(
        "api_key" to config.applicationKey,
        "format" to "json"
    ) + elements

    private fun <T> Result<ByteArray, FuelError>.fromJson(clazz: Class<T>): T = when (this) {
        is Result.Failure -> throw error
        is Result.Success -> {
            log.trace { "Raw result: ${String(value)}" }
            objectMapper.readValue(value, clazz)
        }
    }

    private fun GiantBombDetailsResult.toProviderGameData(detailUrl: String): ProviderFetchResult = ProviderFetchResult(
        type = DataProviderType.GiantBomb,
        detailUrl = detailUrl,

        name = name,
        description = deck,
        releaseDate = originalReleaseDate,

        criticScore = null,
        userScore = null,

        genres = genres.map { it.name },

        imageData = GameImageData(
            thumbnailUrl = image.thumbUrl,
            posterUrl = image.superUrl,
            screenshotUrls = emptyList()    // TODO: Support screenshots
        )
    )
}