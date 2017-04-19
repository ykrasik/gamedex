package com.gitlab.ykrasik.gamedex.provider.igdb

import com.gitlab.ykrasik.gamedex.common.testkit.*
import com.gitlab.ykrasik.gamedex.datamodel.*
import com.gitlab.ykrasik.gamedex.provider.ProviderFetchResult
import com.gitlab.ykrasik.gamedex.provider.ProviderSearchResult
import io.kotlintest.matchers.Matcher
import io.kotlintest.matchers.Matchers
import io.kotlintest.mock.`when`
import io.kotlintest.mock.mock
import org.joda.time.LocalDate

/**
 * User: ykrasik
 * Date: 16/04/2017
 * Time: 15:41
 */
class IgdbDataProviderTest : ScopedWordSpec() {
    init {
        "IgdbDataProvider.search" should {
            "be able to return a single search result".inScope(Scope()) {
                val searchResult = searchResult(name = name, releaseDate = releaseDate, imageId = imageId)

                givenClientSearchReturns(listOf(searchResult), name = name)

                search(name) shouldBe listOf(ProviderSearchResult(
                    apiUrl = "$baseUrl${searchResult.id}",
                    name = name,
                    releaseDate = releaseDate,
                    score = searchResult.aggregatedRating,
                    thumbnailUrl = thumbnailUrl(imageId)
                ))
            }

            "be able to return empty search results".inScope(Scope()) {
                givenClientSearchReturns(emptyList())

                search() shouldBe emptyList<ProviderSearchResult>()
            }

            "be able to return multiple search results".inScope(Scope()) {
                val searchResult1 = searchResult(name)
                val searchResult2 = searchResult("$name ${randomString()}")
                givenClientSearchReturns(listOf(searchResult1, searchResult2), name)

                search(name) should have2SearchResultsThat { first, second ->
                    first.name shouldBe searchResult1.name
                    second.name shouldBe searchResult2.name
                }
            }

            "filter search results that do not contain all words of the original search term".inScope(Scope()) {
                val name = "must contain all words"

                val searchResult1 = searchResult("must contain all")
                val searchResult2 = searchResult("contain all words")
                val searchResult3 = searchResult("must all words")
                val searchResult4 = searchResult("must contain")
                val searchResult5 = searchResult("case Insensitive must additions ContAin possibly ALL WorDs stuff")
                val searchResult6 = searchResult("words all contain must")

                givenClientSearchReturns(listOf(
                    searchResult1, searchResult2, searchResult3,
                    searchResult4, searchResult5, searchResult6
                ), name)

                search(name) should have2SearchResultsThat { first, second ->
                    first.name shouldBe searchResult5.name
                    second.name shouldBe searchResult6.name
                }
            }

            "handle null aggregatedRating".inScope(Scope()) {
                givenClientSearchReturns(listOf(searchResult().copy(aggregatedRating = null)))

                search() should haveSearchResultThat {
                    it.score shouldBe null
                }
            }

            "handle null releaseDates".inScope(Scope()) {
                givenClientSearchReturns(listOf(searchResult().copy(releaseDates = null)))

                search() should haveSearchResultThat {
                    it.releaseDate shouldBe null
                }
            }

            "use null releaseDate when no releaseDate exists for given platform".inScope(Scope()) {
                givenClientSearchReturns(listOf(searchResult(releaseDatePlatformId = platformId + 1)))

                search() should haveSearchResultThat {
                    it.releaseDate shouldBe null
                }
            }

            "handle null thumbnailId".inScope(Scope()) {
                givenClientSearchReturns(listOf(searchResult(imageId = null)))

                search() should haveSearchResultThat {
                    it.thumbnailUrl shouldBe null
                }
            }

            "handle null cover".inScope(Scope()) {
                givenClientSearchReturns(listOf(searchResult().copy(cover = null)))

                search() should haveSearchResultThat {
                    it.thumbnailUrl shouldBe null
                }
            }
        }

        "IgdbDataProvider.fetch" should {
            "fetch a search result".inScope(Scope()) {
                val detailsResult = detailsResult(releaseDate = releaseDate, imageId = imageId)

                givenClientFetchReturns(detailsResult, apiUrl = baseUrl)

                fetch(baseUrl) shouldBe ProviderFetchResult(
                    providerData = ProviderData(
                        type = DataProviderType.Igdb,
                        apiUrl = baseUrl,
                        url = detailsResult.url
                    ),
                    gameData = GameData(
                        name = detailsResult.name,
                        description = detailsResult.summary,
                        releaseDate = releaseDate,
                        criticScore = detailsResult.aggregatedRating,
                        userScore = detailsResult.rating,
                        genres = listOf(genre)
                    ),
                    imageUrls = ImageUrls(
                        thumbnailUrl = thumbnailUrl(imageId),
                        posterUrl = posterUrl(imageId),
                        screenshotUrls = emptyList()
                    )
                )
            }

            "handle null summary".inScope(Scope()) {
                givenClientFetchReturns(detailsResult().copy(summary = null))

                fetch().gameData.description shouldBe null
            }

            "handle null releaseDates".inScope(Scope()) {
                givenClientFetchReturns(detailsResult().copy(releaseDates = null))

                fetch().gameData.releaseDate shouldBe null
            }

            "use null releaseDate when no releaseDate exists for given platform".inScope(Scope()) {
                givenClientFetchReturns(detailsResult(releaseDatePlatformId = platformId + 1))

                fetch().gameData.releaseDate shouldBe null
            }

            "handle null aggregatedRating".inScope(Scope()) {
                givenClientFetchReturns(detailsResult().copy(aggregatedRating = null))

                fetch().gameData.criticScore shouldBe null
            }

            "handle null rating".inScope(Scope()) {
                givenClientFetchReturns(detailsResult().copy(rating = null))

                fetch().gameData.userScore shouldBe null
            }

            "handle null genres".inScope(Scope()) {
                givenClientFetchReturns(detailsResult().copy(genres = null))

                fetch().gameData.genres shouldBe emptyList<String>()
            }

            "handle null imageId".inScope(Scope()) {
                givenClientFetchReturns(detailsResult(imageId = null))

                fetch().imageUrls.thumbnailUrl shouldBe null
                fetch().imageUrls.posterUrl shouldBe null
            }

            "handle null cover".inScope(Scope()) {
                givenClientFetchReturns(detailsResult().copy(cover = null))

                fetch().imageUrls.thumbnailUrl shouldBe null
                fetch().imageUrls.posterUrl shouldBe null
            }
        }
    }

    class Scope : Matchers {
        val platform = randomEnum<GamePlatform>()
        val platformId = rnd.nextInt(100)
        val genreId = rnd.nextInt(100)
        val genre = randomString()
        val name = randomName()
        val releaseDate = randomLocalDate()
        val imageId = randomString()

        val baseUrl = randomUrl()
        val baseImageUrl = randomUrl()

        fun thumbnailUrl(imageId: String) = "$baseImageUrl/t_thumb_2x/$imageId.png"
        fun posterUrl(imageId: String) = "$baseImageUrl/t_screenshot_huge/$imageId.png"

        fun searchResult(name: String = this.name,
                         releaseDate: LocalDate = randomLocalDate(),
                         imageId: String? = randomString(),
                         releaseDatePlatformId: Int = this.platformId) = Igdb.SearchResult(
            id = rnd.nextInt(),
            name = name,
            aggregatedRating = randomScore(),
            releaseDates = listOf(Igdb.ReleaseDate(
                platform = releaseDatePlatformId,
                category = 0,
                human = releaseDate.toString("YYYY-MMM-dd")
            )),
            cover = Igdb.Image(cloudinaryId = imageId)
        )

        fun detailsResult(releaseDate: LocalDate = randomLocalDate(),
                          releaseDatePlatformId: Int = this.platformId,
                          imageId: String? = randomString()) = Igdb.DetailsResult(
            url = randomString(),
            name = name,
            summary = randomSentence(),
            releaseDates = listOf(Igdb.ReleaseDate(
                platform = releaseDatePlatformId,
                category = 0,
                human = releaseDate.toString("YYYY-MMM-dd")
            )),
            aggregatedRating = randomScore(),
            rating = randomScore(),
            cover = Igdb.Image(cloudinaryId = imageId),
            screenshots = emptyList(), // TODO: Support screenshots
            genres = listOf(genreId)
        )

        fun givenClientSearchReturns(results: List<Igdb.SearchResult>, name: String = this.name) {
            `when`(client.search(name, platform)).thenReturn(results)
        }

        fun givenClientFetchReturns(result: Igdb.DetailsResult, apiUrl: String = baseUrl) {
            `when`(client.fetch(apiUrl)).thenReturn(result)
        }

        fun search(name: String = this.name) = provider.search(name, platform)

        fun fetch(apiUrl: String = baseUrl) = provider.fetch(apiUrl, platform)

        private val client = mock<IgdbClient>()
        val provider = IgdbDataProvider(IgdbConfig(
            endpoint = baseUrl,
            baseImageUrl = baseImageUrl,
            apiKey = randomString(),
            maxSearchResults = rnd.nextInt(),
            platforms = mapOf(platform to platformId),
            genres = mapOf(genreId to genre)
        ), client)

        fun haveSearchResultThat(f: (ProviderSearchResult) -> Unit) = object : Matcher<List<ProviderSearchResult>> {
            override fun test(value: List<ProviderSearchResult>) {
                value should haveSize(1)
                f(value.first())
            }
        }

        fun have2SearchResultsThat(f: (first: ProviderSearchResult, second: ProviderSearchResult) -> Unit) = object : Matcher<List<ProviderSearchResult>> {
            override fun test(value: List<ProviderSearchResult>) {
                value should haveSize(2)
                f(value[0], value[1])
            }
        }
    }
}