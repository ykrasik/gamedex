package com.gitlab.ykrasik.gamedex.provider.igdb

import com.gitlab.ykrasik.gamedex.common.testkit.*
import com.gitlab.ykrasik.gamedex.datamodel.*
import com.gitlab.ykrasik.gamedex.provider.ProviderFetchResult
import com.gitlab.ykrasik.gamedex.provider.ProviderSearchResult
import org.joda.time.LocalDate

/**
 * User: ykrasik
 * Date: 13/04/2017
 * Time: 10:43
 */
class IgdbResultAdapterTest : ScopedWordSpec() {
    init {
        "IgdbResultAdapter.toSearchResults" should {
            "correctly adapt empty Igdb search results".inScope(Scope()) {
                adapter.toSearchResults(emptyList(), name, platform) shouldBe emptyList<ProviderSearchResult>()
            }

            "correctly adapt multiple Igdb search results".inScope(Scope()) {
                val id2 = rnd.nextInt()
                val name2 = "$name ${randomString()}"
                val aggregatedRating2 = randomScore()
                val releaseDate2 = randomLocalDate()
                val thumbnailId2 = randomString()

                val searchResults = listOf(
                    Igdb.SearchResult(
                        id = id,
                        name = name,
                        aggregatedRating = aggregatedRating,
                        releaseDates = listOf(releaseDate.toIgdbReleaseDate(platformId)),
                        cover = Igdb.Image(cloudinaryId = thumbnailId)
                    ),
                    Igdb.SearchResult(
                        id = id2,
                        name = name2,
                        aggregatedRating = aggregatedRating2,
                        releaseDates = listOf(releaseDate2.toIgdbReleaseDate(platformId)),
                        cover = Igdb.Image(cloudinaryId = thumbnailId2)
                    )
                )

                adapter.toSearchResults(searchResults, name, platform) shouldBe listOf(
                    ProviderSearchResult(
                        apiUrl = apiUrl(id),
                        name = name,
                        releaseDate = releaseDate,
                        score = aggregatedRating,
                        thumbnailUrl = thumbnailUrl(thumbnailId)
                    ),
                    ProviderSearchResult(
                        apiUrl = apiUrl(id2),
                        name = name2,
                        releaseDate = releaseDate2,
                        score = aggregatedRating2,
                        thumbnailUrl = thumbnailUrl(thumbnailId2)
                    )
                )
            }

            "filter results that do not contain all words of the original search term".inScope(Scope()) {
                val name = "must contain all words"
                val chosenName = "case Insensitive must ContAin ALL WorDs with possible additions"
                val id2 = rnd.nextInt()
                val id3 = rnd.nextInt()
                val id4 = rnd.nextInt()
                val id5 = rnd.nextInt()
                val aggregatedRating2 = randomScore()
                val aggregatedRating3 = randomScore()
                val aggregatedRating4 = randomScore()
                val aggregatedRating5 = randomScore()
                val releaseDate2 = randomLocalDate()
                val releaseDate3 = randomLocalDate()
                val releaseDate4 = randomLocalDate()
                val releaseDate5 = randomLocalDate()
                val thumbnailId2 = randomString()
                val thumbnailId3 = randomString()
                val thumbnailId4 = randomString()
                val thumbnailId5 = randomString()

                val searchResults = listOf(
                    Igdb.SearchResult(
                        id = id,
                        name = "must contain all",
                        aggregatedRating = aggregatedRating,
                        releaseDates = listOf(releaseDate.toIgdbReleaseDate(platformId)),
                        cover = Igdb.Image(cloudinaryId = thumbnailId)
                    ),
                    Igdb.SearchResult(
                        id = id2,
                        name = "contain all words",
                        aggregatedRating = aggregatedRating2,
                        releaseDates = listOf(releaseDate2.toIgdbReleaseDate(platformId)),
                        cover = Igdb.Image(cloudinaryId = thumbnailId2)
                    ),
                    Igdb.SearchResult(
                        id = id3,
                        name = "must all words",
                        aggregatedRating = aggregatedRating3,
                        releaseDates = listOf(releaseDate3.toIgdbReleaseDate(platformId)),
                        cover = Igdb.Image(cloudinaryId = thumbnailId3)
                    ),
                    Igdb.SearchResult(
                        id = id4,
                        name = "must contain",
                        aggregatedRating = aggregatedRating4,
                        releaseDates = listOf(releaseDate4.toIgdbReleaseDate(platformId)),
                        cover = Igdb.Image(cloudinaryId = thumbnailId4)
                    ),
                    Igdb.SearchResult(
                        id = id5,
                        name = chosenName,
                        aggregatedRating = aggregatedRating5,
                        releaseDates = listOf(releaseDate5.toIgdbReleaseDate(platformId)),
                        cover = Igdb.Image(cloudinaryId = thumbnailId5)
                    )
                )

                adapter.toSearchResults(searchResults, name, platform) shouldBe listOf(
                    ProviderSearchResult(
                        apiUrl = apiUrl(id5),
                        name = chosenName,
                        releaseDate = releaseDate5,
                        score = aggregatedRating5,
                        thumbnailUrl = thumbnailUrl(thumbnailId5)
                    )
                )
            }

            "correctly handle results with nullable fields".inScope(Scope()) {
                val searchResults = listOf(Igdb.SearchResult(
                    id = id,
                    name = name,
                    aggregatedRating = null,
                    releaseDates = null,
                    cover = null
                ))

                adapter.toSearchResults(searchResults, name, platform) shouldBe listOf(
                    ProviderSearchResult(
                        apiUrl = apiUrl(id),
                        name = name,
                        releaseDate = null,
                        score = null,
                        thumbnailUrl = null
                    )
                )
            }

            "correctly handle null cover cloudinaryId".inScope(Scope()) {
                val searchResults = listOf(Igdb.SearchResult(
                    id = id,
                    name = name,
                    aggregatedRating = aggregatedRating,
                    releaseDates = listOf(releaseDate.toIgdbReleaseDate(platformId)),
                    cover = Igdb.Image(cloudinaryId = null)
                ))

                // TODO: Use a matcher instead.
                adapter.toSearchResults(searchResults, name, platform) shouldBe listOf(
                    ProviderSearchResult(
                        apiUrl = apiUrl(id),
                        name = name,
                        releaseDate = releaseDate,
                        score = aggregatedRating,
                        thumbnailUrl = null
                    )
                )
            }
        }

        "IgdbResultAdapter.toFetchResult" should {
            "correctly handle results with nullable fields".inScope(Scope()) {
                val fullApiUrl = apiUrl(id)
                val searchResult = ProviderSearchResult(
                    apiUrl = fullApiUrl,
                    name = name,
                    releaseDate = null,
                    score = null,
                    thumbnailUrl = null
                )

                val fetchResult = Igdb.DetailsResult(
                    url = siteDetailUrl,
                    summary = null,
                    rating = null,
                    cover = null,
                    screenshots = emptyList<Igdb.Image>(), // TODO: Support screenshots
                    genres = null
                )

                // TODO: Use a matcher
                adapter.toFetchResult(fetchResult, searchResult) shouldBe ProviderFetchResult(
                    providerData = ProviderData(
                        type = DataProviderType.Igdb,
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

            "retrieve a search result's detailUrl with a null posterId".inScope(Scope()) {
                val fullApiUrl = apiUrl(id)
                val searchResult = ProviderSearchResult(
                    apiUrl = fullApiUrl,
                    name = name,
                    releaseDate = releaseDate,
                    score = aggregatedRating,
                    thumbnailUrl = thumbnailUrl(thumbnailId)
                )

                val fetchResult = Igdb.DetailsResult(
                    url = siteDetailUrl,
                    summary = summary,
                    rating = rating,
                    cover = Igdb.Image(cloudinaryId = null),
                    screenshots = emptyList<Igdb.Image>(), // TODO: Support screenshots
                    genres = listOf(genreId)
                )

                // TODO: Use a matcher
                adapter.toFetchResult(fetchResult, searchResult) shouldBe ProviderFetchResult(
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
                        posterUrl = null,
                        screenshotUrls = emptyList()
                    )
                )
            }
        }
    }

    class Scope {
        val platform = randomEnum<GamePlatform>()
        val platformId = rnd.nextInt(100)
        val genreId = rnd.nextInt(100)
        val genre = randomString()
        val id = rnd.nextInt()
        val name = randomString()
        val aggregatedRating = randomScore()
        val rating = randomScore()
        val releaseDate = randomLocalDate()
        val thumbnailId = randomString()
        val posterId = randomString()
        val siteDetailUrl = randomString()
        val summary = randomSentence()

        val baseUrl = "someBaseUrl"
        val baseImageUrl = "someBaseImageUrl"

        fun apiUrl(gameId: Int) = "$baseUrl$gameId"
        fun thumbnailUrl(imageId: String) = "$baseImageUrl/t_thumb_2x/$imageId.png"
        fun posterUrl(imageId: String) = "$baseImageUrl/t_screenshot_huge/$imageId.png"
        fun LocalDate.toIgdbReleaseDate(platformId: Int) = Igdb.ReleaseDate(
            platform = platformId,
            category = 0,
            human = this.toString("YYYY-MMM-dd")
        )

        val adapter = IgdbResultAdapter(IgdbConfig(
            endpoint = baseUrl,
            baseImageUrl = baseImageUrl,
            apiKey = randomString(),
            maxSearchResults = rnd.nextInt(),
            platforms = mapOf(platform to platformId),
            genres = mapOf(genreId to genre)
        ))
    }
}