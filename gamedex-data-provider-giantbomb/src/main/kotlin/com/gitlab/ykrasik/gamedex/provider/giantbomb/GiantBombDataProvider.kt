package com.gitlab.ykrasik.gamedex.provider.giantbomb

import com.github.ykrasik.gamedex.common.getResourceAsByteArray
import com.github.ykrasik.gamedex.common.logger
import com.github.ykrasik.gamedex.datamodel.GamePlatform
import com.gitlab.ykrasik.gamedex.provider.*
import com.gitlab.ykrasik.gamedex.provider.giantbomb.client.GiantBombClient
import com.gitlab.ykrasik.gamedex.provider.giantbomb.client.GiantBombDetailsResult
import com.gitlab.ykrasik.gamedex.provider.giantbomb.client.GiantBombSearchResult
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

    override val info = DataProviderInfo("GiantBomb", false, this.getResourceAsByteArray("logo.png"))

    override fun search(name: String, platform: GamePlatform): List<SearchResult> {
        log.info { "Searching for name='$name', platform=$platform..." }
        val response = client.search(name, platform)
        if (!response.isOk()) {
            throw DataProviderException("Search: Invalid status code. name=$name, platform=$platform, statusCode=${response.statusCode}")
        }

        val results = response.results.map { mapSearchResult(it) }
        log.info { "Found ${results.size} results." }
        log.debug { "Results = $results" }
        return results
    }

    private fun mapSearchResult(result: GiantBombSearchResult) = SearchResult(
        detailUrl = result.apiDetailUrl,
        name = result.name,
        releaseDate = result.originalReleaseDate,
        score = null
    )

    override fun fetch(searchResult: SearchResult): ProviderGameData {
        log.info { "Getting info for searchResult=$searchResult..." }
        val detailUrl = searchResult.detailUrl
        val result = client.fetch(detailUrl)
        if (!result.isOk()) {
            if (result.isNotFound()) {
                throw DataProviderException("Game data not found for search result: $searchResult")
            } else {
                throw DataProviderException("Invalid status code: detailUrl=$detailUrl, statusCode=${result.statusCode}")
            }
        }

        // When result is found - GiantBomb returns a Json object.
        // When result is not found, GiantBomb returns an empty Json array [].
        // So 'results' can contain at most a single value.
        val gameData = mapGame(result.results.first(), detailUrl)
        log.info { "Found: $gameData" }
        return gameData
    }

    private fun mapGame(result: GiantBombDetailsResult, detailUrl: String): ProviderGameData {
        return ProviderGameData(
            detailUrl = detailUrl,
            name = result.name,
            description = result.deck,
            releaseDate = result.originalReleaseDate,
            criticScore = null,
            userScore = null,
            thumbnail = null, //getThumbnail(node),
            poster = null, //getPoster(node),
            genres = result.genres.map { it.name }
        )
    }

//    private fun getThumbnail(node: JsonNode): ImageData? = getImageData(node, IMAGE_THUMBNAIL)
//
//    private fun getPoster(node: JsonNode): ImageData? = getImageData(node, IMAGE_POSTER)
//
//    private fun getImageData(node: JsonNode, imageName: String): ImageData? = getRawImageData(node, imageName)?.let { ImageData(it) }
//
//    private fun getRawImageData(node: JsonNode, imageName: String): ByteArray? {
//        val imageUrl = getImageUrl(node, imageName)
//        return UrlUtils.fetchOptionalUrl(imageUrl).orElseNull
//    }
//
//    private fun getImageUrl(node: JsonNode, imageName: String): Opt<String> {
//        val image = getField(node, IMAGE)
//        return image.flatMap({ imageNode -> getString(imageNode, imageName) })
//    }
}