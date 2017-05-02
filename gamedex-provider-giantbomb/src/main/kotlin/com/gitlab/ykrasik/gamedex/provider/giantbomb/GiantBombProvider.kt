package com.gitlab.ykrasik.gamedex.provider.giantbomb

import com.gitlab.ykrasik.gamedex.*
import com.gitlab.ykrasik.gamedex.util.getResourceAsByteArray
import com.gitlab.ykrasik.gamedex.util.logger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 29/05/2016
 * Time: 10:53
 */
@Singleton
class GiantBombProvider @Inject constructor(private val client: GiantBombClient) : GameProvider {
    private val log = logger()

    override fun search(name: String, platform: Platform): List<ProviderSearchResult> {
        log.debug("[$platform] Searching: name='$name'...")
        val response = client.search(name, platform)
        assertOk(response.statusCode)

        val results = response.results.map { it.toProviderSearchResult() }
        log.debug("[$platform] ${results.size} Search results: $results.")
        return results
    }

    private fun GiantBombClient.SearchResult.toProviderSearchResult() = ProviderSearchResult(
        name = name,
        releaseDate = originalReleaseDate,
        score = null,
        thumbnailUrl = image?.thumbUrl,
        apiUrl = apiDetailUrl
    )

    override fun fetch(apiUrl: String, platform: Platform): RawGameData {
        log.debug("[$platform] Fetching: $apiUrl...")
        val response = client.fetch(apiUrl)
        assertOk(response.statusCode)

        // When result is found - GiantBomb returns a Json object.
        // When result is not found, GiantBomb returns an empty Json array [].
        // So 'results' can contain at most a single value.
        val gameData = response.results.first().toRawGameData(apiUrl)
        log.debug("[$platform] Result: $gameData.")
        return gameData
    }

    private fun GiantBombClient.DetailsResult.toRawGameData(apiUrl: String) = RawGameData(
        providerData = ProviderData(
            type = GameProviderType.GiantBomb,
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
            screenshotUrls = this.images.map { it.superUrl }
        )
    )

    private fun assertOk(status: GiantBombClient.Status) {
        if (status != GiantBombClient.Status.ok) {
            throw GameDexException("Invalid statusCode: $status")
        }
    }

    override val info = GiantBombProvider.info

    companion object {
        val info = GameProviderInfo(
            name = "GiantBomb",
            type = GameProviderType.GiantBomb,
            logo = getResourceAsByteArray("giantbomb.png")
        )
    }
}