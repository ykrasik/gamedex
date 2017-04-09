package com.gitlab.ykrasik.gamedex.provider.giantbomb

import com.gitlab.ykrasik.gamedex.common.datamodel.*
import com.gitlab.ykrasik.gamedex.common.exception.GameDexException
import com.gitlab.ykrasik.gamedex.common.testkit.*
import com.gitlab.ykrasik.gamedex.provider.ProviderFetchResult
import com.gitlab.ykrasik.gamedex.provider.ProviderSearchResult
import io.kotlintest.matchers.have
import org.eclipse.jetty.http.HttpStatus

/**
 * User: ykrasik
 * Date: 24/03/2017
 * Time: 20:59
 */
class GiantBombDataProviderIT : ScopedWordSpec() {
    val port = 9000

    inner class Scope {
        val apiKey = randomString()
        val platform = randomEnum<GamePlatform>()
        val platformId = rnd.nextInt(100)
        val name = randomString()
        val apiUrl = randomSlashDelimitedString()
        val releaseDate = randomLocalDate()
        val thumbnailUrl = randomUrl()
        val superUrl = randomUrl()
        val siteDetailUrl = randomString()
        val deck = randomSentence()
        val genres = List(4) { GiantBomb.Genre(name = randomString()) }

        val provider = GiantBombDataProvider(GiantBombConfig(
            endpoint = "http://localhost:$port",
            apiKey = apiKey,
            platforms = mapOf(platform to platformId)
        ))
    }

    val server = GiantBombEmbeddedServer(port)

    init {
        "GiantBombDataProvider.search" should {
            "retrieve a single search result".inScope(Scope()) {
                server.searchRequest(apiKey, name, platformId) willReturn GiantBomb.SearchResponse(
                    statusCode = GiantBomb.Status.ok,
                    results = listOf(GiantBomb.SearchResult(
                        apiDetailUrl = apiUrl,
                        name = name,
                        originalReleaseDate = releaseDate,
                        image = GiantBomb.SearchImage(thumbUrl = thumbnailUrl)
                    ))
                )

                val response = provider.search(name, platform)

                response shouldBe listOf(
                    ProviderSearchResult(
                        apiUrl = apiUrl,
                        name = name,
                        releaseDate = releaseDate,
                        score = null,
                        thumbnailUrl = thumbnailUrl
                    )
                )
            }

            "retrieve multiple search results".inScope(Scope()) {
                val apiUrl2 = randomUrl()
                val name2 = "$name ${randomString()}"
                val releaseDate2 = randomLocalDate()
                val thumbnailUrl2 = randomUrl()

                server.searchRequest(apiKey, name, platformId) willReturn GiantBomb.SearchResponse(
                    statusCode = GiantBomb.Status.ok,
                    results = listOf(
                        GiantBomb.SearchResult(
                            apiDetailUrl = apiUrl,
                            name = name,
                            originalReleaseDate = releaseDate,
                            image = GiantBomb.SearchImage(thumbUrl = thumbnailUrl)
                        ),
                        GiantBomb.SearchResult(
                            apiDetailUrl = apiUrl2,
                            name = name2,
                            originalReleaseDate = releaseDate2,
                            image = GiantBomb.SearchImage(thumbUrl = thumbnailUrl2)
                        )
                    )
                )

                val response = provider.search(name, platform)

                response shouldBe listOf(
                    ProviderSearchResult(
                        apiUrl = apiUrl,
                        name = name,
                        releaseDate = releaseDate,
                        score = null,
                        thumbnailUrl = thumbnailUrl
                    ),
                    ProviderSearchResult(
                        apiUrl = apiUrl2,
                        name = name2,
                        releaseDate = releaseDate2,
                        score = null,
                        thumbnailUrl = thumbnailUrl2
                    )
                )
            }

            "retrieve a single search result with null fields".inScope(Scope()) {
                server.searchRequest(apiKey, name, platformId) willReturn GiantBomb.SearchResponse(
                    statusCode = GiantBomb.Status.ok,
                    results = listOf(GiantBomb.SearchResult(
                        apiDetailUrl = apiUrl,
                        name = name,
                        originalReleaseDate = null,
                        image = null
                    ))
                )

                val response = provider.search(name, platform)

                response shouldBe listOf(
                    ProviderSearchResult(
                        apiUrl = apiUrl,
                        name = name,
                        releaseDate = null,
                        score = null,
                        thumbnailUrl = null
                    )
                )
            }

            "not find any search results".inScope(Scope()) {
                server.searchRequest(apiKey, name, platformId) willReturn GiantBomb.SearchResponse(
                    statusCode = GiantBomb.Status.ok,
                    results = emptyList()
                )

                val response = provider.search(name, platform)
                response shouldBe emptyList<ProviderSearchResult>()
            }

            "throw GameDexException on invalid GiantBomb status".inScope(Scope()) {
                server.searchRequest(apiKey, name, platformId) willReturn GiantBomb.SearchResponse(
                    statusCode = GiantBomb.Status.notFound,
                    results = emptyList()
                )

                val e = shouldThrow<GameDexException> {
                    provider.search(name, platform)
                }
                e.message shouldBe "Invalid statusCode: ${GiantBomb.Status.notFound}"
            }

            "throw GameDexException on invalid http status".inScope(Scope()) {
                server.searchRequest(apiKey, name, platformId) willFailWith HttpStatus.BAD_REQUEST_400

                val e = shouldThrow<GameDexException> {
                    provider.search(name, platform)
                }
                e.message!! should have substring HttpStatus.BAD_REQUEST_400.toString()
            }
        }

        "GiantBombDataProvider.fetch" should {
            "retrieve a search result's detailUrl".inScope(Scope()) {
                val fullApiUrl = "http://localhost:$port/$apiUrl"
                val searchResult = ProviderSearchResult(
                    apiUrl = fullApiUrl,
                    name = name,
                    releaseDate = releaseDate,
                    score = null,
                    thumbnailUrl = thumbnailUrl
                )

                server.fetchRequest(apiKey, apiUrl) willReturn GiantBomb.DetailsResponse(
                    statusCode = GiantBomb.Status.ok,
                    results = listOf(GiantBomb.DetailsResult(
                        siteDetailUrl = siteDetailUrl,
                        deck = deck,
                        genres = genres,
                        image = GiantBomb.DetailsImage(
                            thumbUrl = thumbnailUrl,
                            superUrl = superUrl
                        )
                    ))
                )

                val response = provider.fetch(searchResult)

                response shouldBe ProviderFetchResult(
                    providerData = ProviderData(
                        type = DataProviderType.GiantBomb,
                        apiUrl = fullApiUrl,
                        url = siteDetailUrl
                    ),
                    gameData = GameData(
                        name = name,
                        description = deck,
                        releaseDate = releaseDate,
                        criticScore = null,
                        userScore = null,
                        genres = genres.map { it.name }
                    ),
                    imageUrls = ImageUrls(
                        thumbnailUrl = thumbnailUrl,
                        posterUrl = superUrl,
                        screenshotUrls = emptyList()
                    )
                )
            }

            "retrieve a search result's detailUrl with null fields".inScope(Scope()) {
                val fullApiUrl = "http://localhost:$port/$apiUrl"
                val searchResult = ProviderSearchResult(
                    apiUrl = fullApiUrl,
                    name = name,
                    releaseDate = null,
                    score = null,
                    thumbnailUrl = null
                )

                server.fetchRequest(apiKey, apiUrl) willReturn GiantBomb.DetailsResponse(
                    statusCode = GiantBomb.Status.ok,
                    results = listOf(GiantBomb.DetailsResult(
                        siteDetailUrl = siteDetailUrl,
                        deck = null,
                        genres = null,
                        image = null
                    ))
                )

                val response = provider.fetch(searchResult)

                response shouldBe ProviderFetchResult(
                    providerData = ProviderData(
                        type = DataProviderType.GiantBomb,
                        apiUrl = fullApiUrl,
                        url = siteDetailUrl
                    ),
                    gameData = GameData(
                        name = name,
                        description = null,
                        releaseDate = null,
                        criticScore = null,
                        userScore = null,
                        genres = emptyList()
                    ),
                    imageUrls = ImageUrls(
                        thumbnailUrl = null,
                        posterUrl = null,
                        screenshotUrls = emptyList()
                    )
                )
            }

            "throw GameDexException on invalid GiantBomb status".inScope(Scope()) {
                val fullApiUrl = "http://localhost:$port/$apiUrl"
                val searchResult = ProviderSearchResult(
                    apiUrl = fullApiUrl,
                    name = name,
                    releaseDate = releaseDate,
                    score = null,
                    thumbnailUrl = thumbnailUrl
                )

                server.fetchRequest(apiKey, apiUrl) willReturn GiantBomb.DetailsResponse(
                    statusCode = GiantBomb.Status.notFound,
                    results = emptyList()
                )

                val e = shouldThrow<GameDexException> {
                    provider.fetch(searchResult)
                }
                e.message shouldBe "Invalid statusCode: ${GiantBomb.Status.notFound}"
            }

            "throw GameDexException on invalid http status".inScope(Scope()) {
                val fullApiUrl = "http://localhost:$port/$apiUrl"
                val searchResult = ProviderSearchResult(
                    apiUrl = fullApiUrl,
                    name = name,
                    releaseDate = releaseDate,
                    score = null,
                    thumbnailUrl = thumbnailUrl
                )

                server.fetchRequest(apiKey, apiUrl) willFailWith HttpStatus.BAD_REQUEST_400

                val e = shouldThrow<GameDexException> {
                    provider.fetch(searchResult)
                }
                e.message!! should have substring HttpStatus.BAD_REQUEST_400.toString()
            }
        }
    }

    override fun beforeAll() { server.start() }
    override fun beforeEach() { server.reset() }
}