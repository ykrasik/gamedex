package com.gitlab.ykrasik.gamedex.provider.igdb

import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.gitlab.ykrasik.gamedex.common.exception.GameDexException
import com.gitlab.ykrasik.gamedex.common.testkit.*
import com.gitlab.ykrasik.gamedex.datamodel.*
import com.gitlab.ykrasik.gamedex.provider.ProviderFetchResult
import com.gitlab.ykrasik.gamedex.provider.ProviderSearchResult
import com.gitlab.ykrasik.provider.testkit.withHeader
import com.gitlab.ykrasik.provider.testkit.withQueryParam
import io.kotlintest.matchers.have
import org.eclipse.jetty.http.HttpStatus
import org.joda.time.LocalDate

/**
 * User: ykrasik
 * Date: 02/04/2017
 * Time: 21:02
 */
class IgdbDataProviderIT : ScopedWordSpec() {
    val port = 9001
    val server = IgdbMockServer(port)

    init {
        "IgdbProvider.search" should {
            "retrieve a single search result".inScope(Scope()) {
                server.aSearchRequest() willReturn listOf(Igdb.SearchResult(
                    id = id,
                    name = name,
                    aggregatedRating = aggregatedRating,
                    releaseDates = listOf(releaseDate.toIgdbReleaseDate(platformId)),
                    cover = Igdb.Image(cloudinaryId = thumbnailId)
                ))

                val results = provider.search(name, platform)

                results shouldBe listOf(ProviderSearchResult(
                    apiUrl = apiUrl(id),
                    name = name,
                    releaseDate = releaseDate,
                    score = aggregatedRating,
                    thumbnailUrl = thumbnailUrl(thumbnailId)
                ))

                server.verify(
                    getRequestedFor(urlPathEqualTo("/"))
                        .withHeader("Accept", "application/json")
                        .withHeader("X-Mashape-Key", apiKey)
                        .withQueryParam("search", name)
                        .withQueryParam("filter[release_dates.platform][eq]", platformId.toString())
                        .withQueryParam("limit", maxSearchResults.toString())
                        .withQueryParam("fields", searchFields)
                )
            }

            "throw GameDexException on invalid http status".inScope(Scope()) {
                server.aSearchRequest() willFailWith HttpStatus.BAD_REQUEST_400

                val e = shouldThrow<GameDexException> {
                    provider.search(name, platform)
                }
                e.message!! should have substring HttpStatus.BAD_REQUEST_400.toString()
            }
        }

        "IgdbDataProvider.fetch" should {
            "retrieve a search result's detailUrl".inScope(Scope()) {
                val fullApiUrl = apiUrl(id)
                val searchResult = ProviderSearchResult(
                    apiUrl = fullApiUrl,
                    name = name,
                    releaseDate = releaseDate,
                    score = aggregatedRating,
                    thumbnailUrl = thumbnailUrl(thumbnailId)
                )

                server.aFetchRequest(id) willReturn Igdb.DetailsResult(
                    url = siteDetailUrl,
                    summary = summary,
                    rating = rating,
                    cover = Igdb.Image(cloudinaryId = posterId),
                    screenshots = emptyList<Igdb.Image>(), // TODO: Support screenshots
                    genres = listOf(genreId)
                )

                val response = provider.fetch(searchResult)

                response shouldBe ProviderFetchResult(
                    providerData = ProviderData(
                        type = DataProviderType.Igdb,
                        apiUrl = fullApiUrl,
                        url = siteDetailUrl
                    ),
                    gameData = GameData(
                        name = name,
                        description = summary,
                        releaseDate = releaseDate,
                        criticScore = aggregatedRating,
                        userScore = rating,
                        genres = listOf(genre)
                    ),
                    imageUrls = ImageUrls(
                        thumbnailUrl = thumbnailUrl(thumbnailId),
                        posterUrl = posterUrl(posterId),
                        screenshotUrls = emptyList()
                    )
                )

                server.verify(
                    getRequestedFor(urlPathEqualTo("/$id"))
                        .withHeader("Accept", "application/json")
                        .withHeader("X-Mashape-Key", apiKey)
                        .withQueryParam("fields", fetchDetailsFields)
                )
            }

            "throw GameDexException on invalid http status".inScope(Scope()) {
                val fullApiUrl = apiUrl(id)
                val searchResult = ProviderSearchResult(
                    apiUrl = fullApiUrl,
                    name = name,
                    releaseDate = releaseDate,
                    score = aggregatedRating,
                    thumbnailUrl = thumbnailUrl(thumbnailId)
                )

                server.aFetchRequest(id) willFailWith HttpStatus.BAD_REQUEST_400

                val e = shouldThrow<GameDexException> {
                    provider.fetch(searchResult)
                }
                e.message!! should have substring HttpStatus.BAD_REQUEST_400.toString()
            }
        }
    }

    inner class Scope {
        val apiKey = randomString()
        val platform = randomEnum<GamePlatform>()
        val platformId = rnd.nextInt(100)
        val genreId = rnd.nextInt(100)
        val genre = randomString()
        val maxSearchResults = rnd.nextInt()
        val id = rnd.nextInt()
        val name = randomString()
        val aggregatedRating = randomScore()
        val rating = randomScore()
        val releaseDate = randomLocalDate()
        val thumbnailId = randomString()
        val posterId = randomString()
        val siteDetailUrl = randomString()
        val summary = randomSentence()

        val baseUrl = "http://localhost:$port/"
        val baseImageUrl = "$baseUrl/images"
        val provider = IgdbDataProvider(IgdbConfig(
            endpoint = baseUrl,
            baseImageUrl = baseImageUrl,
            apiKey = apiKey,
            maxSearchResults = maxSearchResults,
            platforms = mapOf(platform to platformId),
            genres = mapOf(genreId to genre)
        ))

        fun apiUrl(gameId: Int) = "$baseUrl$gameId"
        fun thumbnailUrl(imageId: String) = "$baseImageUrl/t_thumb_2x/$imageId.png"
        fun posterUrl(imageId: String) = "$baseImageUrl/t_screenshot_huge/$imageId.png"
        fun LocalDate.toIgdbReleaseDate(platformId: Int) = Igdb.ReleaseDate(
            platform = platformId,
            category = 0,
            human = this.toString("YYYY-MMM-dd")
        )
    }

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
        "rating",
        "cover.cloudinary_id",
        "screenshots.cloudinary_id",
        "genres"
    ).joinToString(",")

    override fun beforeAll(): Unit = server.start()
    override fun beforeEach(): Unit = server.reset()
}