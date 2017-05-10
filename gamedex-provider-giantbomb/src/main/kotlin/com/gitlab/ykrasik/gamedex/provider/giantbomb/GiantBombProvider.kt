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
        log.debug("[$platform] Searching: '$name'...")
        val response = client.search(name, platform)
        assertOk(response.statusCode)

        val results = response.results.map { it.toProviderSearchResult() }
        log.debug("[$platform] Searching: '$name': ${results.size} results.")
        results.forEach { log.trace("[$platform] $it") }
        return results
    }

    private fun GiantBombClient.SearchResult.toProviderSearchResult() = ProviderSearchResult(
        name = name,
        releaseDate = originalReleaseDate,
        score = null,
        thumbnailUrl = image?.thumbUrl,
        apiUrl = apiDetailUrl
    )

    override fun fetch(apiUrl: String, platform: Platform): ProviderData {
        log.debug("[$platform] Fetching: $apiUrl...")
        val response = client.fetch(apiUrl)
        assertOk(response.statusCode)

        // When result is found - GiantBomb returns a Json object.
        // When result is not found, GiantBomb returns an empty Json array [].
        // So 'results' can contain at most a single value.
        val gameData = response.results.first().toProviderData(apiUrl)
        log.debug("[$platform] Result: $gameData.")
        return gameData
    }

    private fun GiantBombClient.DetailsResult.toProviderData(apiUrl: String) = ProviderData(
        header = ProviderHeader(
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

    override val name = "GiantBomb"
    override val type = GameProviderType.GiantBomb
    override val logo = GiantBombProvider.logo

    companion object {
        val logo = getResourceAsByteArray("giantbomb.png")
    }
}