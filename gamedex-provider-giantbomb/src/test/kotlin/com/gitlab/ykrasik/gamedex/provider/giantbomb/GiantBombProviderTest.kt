package com.gitlab.ykrasik.gamedex.provider.giantbomb

import com.gitlab.ykrasik.gamedex.*
import com.gitlab.ykrasik.gamedex.test.*
import io.kotlintest.matchers.*
import io.kotlintest.mock.`when`
import io.kotlintest.mock.mock

/**
 * User: ykrasik
 * Date: 19/04/2017
 * Time: 09:21
 */
class GiantBombProviderTest : ScopedWordSpec() {
    init {
        "GiantBombDataProvider.search" should {
            "be able to return a single search result".inScope(Scope()) {
                val searchResult = searchResult(name = name)

                givenClientSearchReturns(listOf(searchResult), name = name)

                search(name) shouldBe listOf(ProviderSearchResult(
                    apiUrl = searchResult.apiDetailUrl,
                    name = name,
                    releaseDate = searchResult.originalReleaseDate,
                    score = null,
                    thumbnailUrl = searchResult.image!!.thumbUrl
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

            "handle null originalReleaseDate".inScope(Scope()) {
                givenClientSearchReturns(listOf(searchResult().copy(originalReleaseDate = null)))

                search() should haveASingleSearchResultThat {
                    it.score shouldBe null
                }
            }

            "handle null image".inScope(Scope()) {
                givenClientSearchReturns(listOf(searchResult().copy(image = null)))

                search() should haveASingleSearchResultThat {
                    it.thumbnailUrl shouldBe null
                }
            }

            "throw GameDexException on invalid response status".inScope(Scope()) {
                givenClientSearchReturns(GiantBombClient.SearchResponse(GiantBombClient.Status.badFormat, emptyList()))

                shouldThrow<GameDexException> {
                    search()
                }
            }
        }

        "GiantBombDataProvider.fetch" should {
            "fetch a search result".inScope(Scope()) {
                val detailsResult = detailsResult()

                givenClientFetchReturns(detailsResult, apiUrl = apiDetailUrl)

                fetch(apiDetailUrl) shouldBe RawGameData(
                    providerData = ProviderData(
                        type = GameProviderType.GiantBomb,
                        apiUrl = apiDetailUrl,
                        siteUrl = detailsResult.siteDetailUrl
                    ),
                    gameData = GameData(
                        name = detailsResult.name,
                        description = detailsResult.deck,
                        releaseDate = detailsResult.originalReleaseDate,
                        criticScore = null,
                        userScore = null,
                        genres = listOf(detailsResult.genres!!.first().name)
                    ),
                    imageUrls = ImageUrls(
                        thumbnailUrl = detailsResult.image!!.thumbUrl,
                        posterUrl = detailsResult.image!!.superUrl,
                        screenshotUrls = emptyList()
                    )
                )
            }

            "handle null deck".inScope(Scope()) {
                givenClientFetchReturns(detailsResult().copy(deck = null))

                fetch().gameData.description shouldBe null
            }

            "handle null originalReleaseDate".inScope(Scope()) {
                givenClientFetchReturns(detailsResult().copy(originalReleaseDate = null))

                fetch().gameData.releaseDate shouldBe null
            }

            "handle null genres".inScope(Scope()) {
                givenClientFetchReturns(detailsResult().copy(genres = null))

                fetch().gameData.genres shouldBe emptyList<String>()
            }

            "handle null image".inScope(Scope()) {
                givenClientFetchReturns(detailsResult().copy(image = null))

                fetch().imageUrls.thumbnailUrl shouldBe null
                fetch().imageUrls.posterUrl shouldBe null
            }

            "throw GameDexException on invalid response status".inScope(Scope()) {
                givenClientFetchReturns(GiantBombClient.DetailsResponse(GiantBombClient.Status.badFormat, emptyList()))

                shouldThrow<GameDexException> {
                    fetch()
                }
            }
        }
    }

    class Scope {
        val platform = randomEnum<GamePlatform>()
        val name = randomName()
        val apiDetailUrl = randomUrl()

        fun searchResult(name: String = this.name) = GiantBombClient.SearchResult(
            apiDetailUrl = randomUrl(),
            name = name,
            originalReleaseDate = randomLocalDate(),
            image = GiantBombClient.SearchImage(thumbUrl = randomUrl())
        )

        fun detailsResult(name: String = this.name) = GiantBombClient.DetailsResult(
            siteDetailUrl = randomUrl(),
            name = name,
            deck = randomSentence(),
            originalReleaseDate = randomLocalDate(),
            image = GiantBombClient.DetailsImage(thumbUrl = randomUrl(), superUrl = randomUrl()),
            genres = listOf(GiantBombClient.Genre(randomString()))
        )

        fun givenClientSearchReturns(results: List<GiantBombClient.SearchResult>, name: String = this.name) =
            givenClientSearchReturns(GiantBombClient.SearchResponse(GiantBombClient.Status.ok, results), name)

        fun givenClientSearchReturns(response: GiantBombClient.SearchResponse, name: String = this.name) {
            `when`(client.search(name, platform)).thenReturn(response)
        }

        fun givenClientFetchReturns(result: GiantBombClient.DetailsResult, apiUrl: String = apiDetailUrl) =
            givenClientFetchReturns(GiantBombClient.DetailsResponse(GiantBombClient.Status.ok, listOf(result)), apiUrl)

        fun givenClientFetchReturns(response: GiantBombClient.DetailsResponse, apiUrl: String = apiDetailUrl) {
            `when`(client.fetch(apiUrl)).thenReturn(response)
        }

        fun search(name: String = this.name) = provider.search(name, platform)

        fun fetch(apiUrl: String = apiDetailUrl, platform: GamePlatform = this.platform) = provider.fetch(apiUrl, platform)

        private val client = mock<GiantBombClient>()
        val provider = GiantBombProvider(client)

        fun haveASingleSearchResultThat(f: (ProviderSearchResult) -> Unit) = object : Matcher<List<ProviderSearchResult>> {
            override fun test(value: List<ProviderSearchResult>): Result {
                value should haveSize(1)
                f(value.first())
                return Result(true, "")
            }
        }

        fun have2SearchResultsThat(f: (first: ProviderSearchResult, second: ProviderSearchResult) -> Unit) = object : Matcher<List<ProviderSearchResult>> {
            override fun test(value: List<ProviderSearchResult>): Result {
                value should haveSize(2)
                f(value[0], value[1])
                return Result(true, "")
            }
        }
    }
}