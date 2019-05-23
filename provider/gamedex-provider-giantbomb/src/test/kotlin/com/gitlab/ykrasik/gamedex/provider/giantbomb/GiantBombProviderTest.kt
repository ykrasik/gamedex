/****************************************************************************
 * Copyright (C) 2016-2019 Yevgeny Krasik                                   *
 *                                                                          *
 * Licensed under the Apache License, Version 2.0 (the "License");          *
 * you may not use this file except in compliance with the License.         *
 * You may obtain a copy of the License at                                  *
 *                                                                          *
 * http://www.apache.org/licenses/LICENSE-2.0                               *
 *                                                                          *
 * Unless required by applicable law or agreed to in writing, software      *
 * distributed under the License is distributed on an "AS IS" BASIS,        *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. *
 * See the License for the specific language governing permissions and      *
 * limitations under the License.                                           *
 ****************************************************************************/

package com.gitlab.ykrasik.gamedex.provider.giantbomb

import com.gitlab.ykrasik.gamedex.GameData
import com.gitlab.ykrasik.gamedex.ImageUrls
import com.gitlab.ykrasik.gamedex.Platform
import com.gitlab.ykrasik.gamedex.provider.ProviderFetchData
import com.gitlab.ykrasik.gamedex.provider.ProviderOrderPriorities
import com.gitlab.ykrasik.gamedex.provider.ProviderSearchResult
import com.gitlab.ykrasik.gamedex.test.*
import io.kotlintest.matchers.*
import io.mockk.coEvery
import io.mockk.mockk

/**
 * User: ykrasik
 * Date: 19/04/2017
 * Time: 09:21
 */
class GiantBombProviderTest : ScopedWordSpec<GiantBombProviderTest.Scope>() {
    override fun scope() = Scope()

    init {
        "search" should {
            "be able to return a single search result" test {
                val searchResult = searchResult(name = name)

                givenClientSearchReturns(listOf(searchResult), name = name)

                search(name) shouldBe listOf(
                    ProviderSearchResult(
                        apiUrl = searchResult.apiDetailUrl,
                        name = name,
                        description = description,
                        releaseDate = searchResult.originalReleaseDate?.toString(),
                        criticScore = null,
                        userScore = null,
                        thumbnailUrl = searchResult.image!!.thumbUrl
                    )
                )
            }

            "be able to return empty search results" test {
                givenClientSearchReturns(emptyList())

                search() shouldBe emptyList<ProviderSearchResult>()
            }

            "be able to return multiple search results" test {
                val searchResult1 = searchResult(name)
                val searchResult2 = searchResult("$name ${randomWord()}")
                givenClientSearchReturns(listOf(searchResult1, searchResult2), name)

                search(name) should have2SearchResultsThat { first, second ->
                    first.name shouldBe searchResult1.name
                    second.name shouldBe searchResult2.name
                }
            }

            "handle null deck" test {
                givenClientSearchReturns(listOf(searchResult().copy(deck = null)))

                search() should haveASingleSearchResultThat {
                    it.description shouldBe null
                }
            }

            "handle null originalReleaseDate" test {
                givenClientSearchReturns(listOf(searchResult().copy(originalReleaseDate = null)))

                search() should haveASingleSearchResultThat {
                    it.criticScore shouldBe null
                }
            }

            "handle null image" test {
                givenClientSearchReturns(listOf(searchResult().copy(image = null)))

                search() should haveASingleSearchResultThat {
                    it.thumbnailUrl shouldBe null
                }
            }

            "consider noImageUrl1 as absent image" test {
                givenClientSearchReturns(listOf(searchResult().copy(image = randomImage().copy(thumbUrl = "${randomUrl()}/$noImage1"))))

                search() should haveASingleSearchResultThat {
                    it.thumbnailUrl shouldBe null
                }
            }

            "consider noImageUrl2 as absent image" test {
                givenClientSearchReturns(listOf(searchResult().copy(image = randomImage().copy(thumbUrl = "${randomUrl()}/$noImage2"))))

                search() should haveASingleSearchResultThat {
                    it.thumbnailUrl shouldBe null
                }
            }

            "throw IllegalStateException on invalid response status" test {
                givenClientSearchReturns(GiantBombClient.SearchResponse(GiantBombClient.Status.BadFormat, emptyList()))

                shouldThrow<IllegalStateException> {
                    search()
                }
            }
        }

        "fetch" should {
            "fetch details" test {
                val detailsResult = detailsResult()

                givenClientFetchReturns(detailsResult, apiUrl = apiDetailUrl)

                fetch(apiDetailUrl) shouldBe ProviderFetchData(
                    gameData = GameData(
                        name = detailsResult.name,
                        description = detailsResult.deck,
                        releaseDate = detailsResult.originalReleaseDate?.toString(),
                        criticScore = null,
                        userScore = null,
                        genres = listOf(detailsResult.genres!!.first().name),
                        imageUrls = ImageUrls(
                            thumbnailUrl = detailsResult.image!!.thumbUrl,
                            posterUrl = detailsResult.image!!.superUrl,
                            screenshotUrls = detailsResult.images.map { it.superUrl }
                        )
                    ),
                    siteUrl = detailsResult.siteDetailUrl
                )
            }

            "handle null deck" test {
                givenClientFetchReturns(detailsResult().copy(deck = null))

                fetch().gameData.description shouldBe null
            }

            "handle null originalReleaseDate" test {
                givenClientFetchReturns(detailsResult().copy(originalReleaseDate = null))

                fetch().gameData.releaseDate shouldBe null
            }

            "handle null genres" test {
                givenClientFetchReturns(detailsResult().copy(genres = null))

                fetch().gameData.genres shouldBe emptyList<String>()
            }

            "handle null image" test {
                givenClientFetchReturns(detailsResult().copy(image = null))

                fetch().gameData.imageUrls.thumbnailUrl shouldBe null
                fetch().gameData.imageUrls.posterUrl shouldBe null
            }

            "consider noImageUrl1 as absent thumbnail" test {
                givenClientFetchReturns(detailsResult().copy(image = randomImage().copy(thumbUrl = "${randomUrl()}/$noImage1")))

                fetch().gameData.imageUrls.thumbnailUrl shouldBe null
            }

            "consider noImageUrl2 as absent thumbnail" test {
                givenClientFetchReturns(detailsResult().copy(image = randomImage().copy(thumbUrl = "${randomUrl()}/$noImage2")))

                fetch().gameData.imageUrls.thumbnailUrl shouldBe null
            }

            "consider noImageUrl1 as absent poster" test {
                givenClientFetchReturns(detailsResult().copy(image = randomImage().copy(superUrl = "${randomUrl()}/$noImage1")))

                fetch().gameData.imageUrls.posterUrl shouldBe null
            }

            "consider noImageUrl1 as absent poster" test {
                givenClientFetchReturns(detailsResult().copy(image = randomImage().copy(superUrl = "${randomUrl()}/$noImage2")))

                fetch().gameData.imageUrls.posterUrl shouldBe null
            }

            "filter out no image urls from screenshots" test {
                val screenshot1 = randomImage()
                givenClientFetchReturns(
                    detailsResult().copy(
                        images = listOf(
                            randomImage().copy(superUrl = "${randomUrl()}/$noImage1"),
                            screenshot1,
                            randomImage().copy(superUrl = "${randomUrl()}/$noImage2")
                        )
                    )
                )

                fetch().gameData.imageUrls.screenshotUrls shouldBe listOf(screenshot1.superUrl)
            }

            "throw IllegalStateException on invalid response status" test {
                givenClientFetchReturns(GiantBombClient.DetailsResponse(GiantBombClient.Status.BadFormat, emptyList()))

                shouldThrow<IllegalStateException> {
                    fetch()
                }
            }
        }
    }

    class Scope {
        val platform = randomEnum<Platform>()
        val name = randomName()
        val description = randomParagraph()
        val apiDetailUrl = randomUrl()
        val account = GiantBombUserAccount(apiKey = randomWord())
        val noImage1 = randomWord()
        val noImage2 = randomWord()

        fun randomImage() = GiantBombClient.Image(thumbUrl = randomUrl(), superUrl = randomUrl())

        fun searchResult(name: String = this.name) = GiantBombClient.SearchResult(
            apiDetailUrl = randomUrl(),
            name = name,
            deck = description,
            originalReleaseDate = randomLocalDate(),
            image = randomImage()
        )

        fun detailsResult(name: String = this.name) = GiantBombClient.DetailsResult(
            siteDetailUrl = randomUrl(),
            name = name,
            deck = randomParagraph(),
            originalReleaseDate = randomLocalDate(),
            image = randomImage(),
            images = listOf(randomImage(), randomImage()),
            genres = listOf(GiantBombClient.Genre(randomWord()))
        )

        fun givenClientSearchReturns(results: List<GiantBombClient.SearchResult>, name: String = this.name) =
            givenClientSearchReturns(GiantBombClient.SearchResponse(GiantBombClient.Status.OK, results), name)

        fun givenClientSearchReturns(response: GiantBombClient.SearchResponse, name: String = this.name) {
            coEvery { client.search(name, platform, account) } returns response
        }

        fun givenClientFetchReturns(result: GiantBombClient.DetailsResult, apiUrl: String = apiDetailUrl) =
            givenClientFetchReturns(GiantBombClient.DetailsResponse(GiantBombClient.Status.OK, listOf(result)), apiUrl)

        fun givenClientFetchReturns(response: GiantBombClient.DetailsResponse, apiUrl: String = apiDetailUrl) {
            coEvery { client.fetch(apiUrl, account) } returns response
        }

        suspend fun search(name: String = this.name) = provider.search(name, platform, account)
        suspend fun fetch(apiUrl: String = apiDetailUrl, platform: Platform = this.platform) = provider.fetch(apiUrl, platform, account)

        private val config = GiantBombConfig("", listOf(noImage1, noImage2), "", ProviderOrderPriorities.default, emptyMap())
        private val client = mockk<GiantBombClient>()
        val provider = GiantBombProvider(config, client)

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