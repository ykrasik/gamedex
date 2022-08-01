/****************************************************************************
 * Copyright (C) 2016-2020 Yevgeny Krasik                                   *
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

import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.gitlab.ykrasik.gamedex.GameData
import com.gitlab.ykrasik.gamedex.Platform
import com.gitlab.ykrasik.gamedex.provider.GameProvider
import com.gitlab.ykrasik.gamedex.test.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.ktor.client.plugins.*
import io.ktor.http.*

/**
 * User: ykrasik
 * Date: 19/04/2017
 * Time: 09:21
 */
class GiantBombProviderTest : Spec<GiantBombProviderTest.Scope>() {
    init {
        describe("search") {
            itShould("be able to return a single search result") {
                val query = "query"
                val result = searchResult()
                givenSearchResults(result)

                provider.search(query, platform, account, offset, limit) shouldBe GameProvider.SearchResponse(
                    results = listOf(
                        GameProvider.SearchResult(
                            providerGameId = result.apiDetailUrl,
                            name = result.name,
                            description = result.deck,
                            releaseDate = result.originalReleaseDate!!.toString(),
                            criticScore = null,
                            userScore = null,
                            thumbnailUrl = result.image!!.thumbUrl
                        )
                    ),
                    canShowMoreResults = null
                )

                server.verify {
                    getRequestedFor(urlPathEqualTo("/"))
                        .withQueryParam("api_key", account.apiKey)
                        .withQueryParam("format", "json")
                        .withQueryParam("filter", "name:$query,platforms:$platformId")
                        .withQueryParam(
                            "field_list",
                            "api_detail_url,name,deck,original_release_date,expected_release_year," +
                                    "expected_release_quarter,expected_release_month,expected_release_day,image"
                        )
                        .withQueryParam("offset", "$offset")
                        .withQueryParam("limit", "$limit")
                }
            }

            itShould("be able to return empty search results") {
                givenSearchResults()

                search() shouldBe emptyList()
            }

            itShould("be able to return multiple search results") {
                val result1 = searchResult()
                val result2 = searchResult()
                givenSearchResults(result1, result2)

                search() should have2SearchResultsWhere { first, second ->
                    first.name shouldBe result1.name
                    second.name shouldBe result2.name
                }
            }

            itShould("return null description when 'deck' is null") {
                givenSearchResults(searchResult().copy(deck = null))

                search() should have1SearchResultWhere { description shouldBe null }
            }

            itShould("return null releaseDate when 'originalReleaseDate' & 'expectedReleaseDates' are null") {
                givenSearchResults(
                    searchResult().copy(
                        originalReleaseDate = null,
                        expectedReleaseYear = null,
                        expectedReleaseMonth = null,
                        expectedReleaseDay = null,
                        expectedReleaseQuarter = null
                    )
                )

                search() should have1SearchResultWhere { releaseDate shouldBe null }
            }

            itShould("use expectedReleaseYear, expectedReleaseMonth & expectedReleaseDay as releaseDate when originalReleaseDate is null and those are present") {
                givenSearchResults(
                    searchResult().copy(
                        originalReleaseDate = null,
                        expectedReleaseYear = 2019,
                        expectedReleaseMonth = 7,
                        expectedReleaseDay = 12,
                        expectedReleaseQuarter = null
                    )
                )

                search() should have1SearchResultWhere { releaseDate shouldBe "2019-07-12" }
            }

            itShould("use expectedReleaseYear & expectedReleaseMonth as releaseDate when originalReleaseDate is null and those are present") {
                givenSearchResults(
                    searchResult().copy(
                        originalReleaseDate = null,
                        expectedReleaseYear = 1998,
                        expectedReleaseMonth = 10,
                        expectedReleaseDay = null,
                        expectedReleaseQuarter = null
                    )
                )

                search() should have1SearchResultWhere { releaseDate shouldBe "1998-10-01" }
            }

            "use expectedReleaseYear & expectedReleaseQuarter as releaseDate when originalReleaseDate is null and those are present, " +
                    itShould("even if expectedReleaseMonth & expectedReleaseDay are also present") {
                        givenSearchResults(
                            searchResult().copy(
                                originalReleaseDate = null,
                                expectedReleaseYear = 1998,
                                expectedReleaseMonth = 10,
                                expectedReleaseDay = 12,
                                expectedReleaseQuarter = 1
                            )
                        )

                        search() should have1SearchResultWhere { releaseDate shouldBe "1998 Q1" }
                    }

            "return null releaseDate if originalReleaseDate & expectedReleaseYear are null," +
                    itShould("even if expectedReleaseMonth & expectedReleaseDay are also present") {
                        givenSearchResults(
                            searchResult().copy(
                                originalReleaseDate = null,
                                expectedReleaseYear = null,
                                expectedReleaseMonth = 10,
                                expectedReleaseDay = 12,
                                expectedReleaseQuarter = 1
                            )
                        )

                        search() should have1SearchResultWhere { releaseDate shouldBe null }
                    }

            itShould("return null thumbnailUrl when 'image' is null") {
                givenSearchResults(searchResult().copy(image = null))

                search() should have1SearchResultWhere { thumbnailUrl shouldBe null }
            }

            itShould("consider noImageUrl1 as absent image") {
                givenSearchResults(searchResult().copy(image = randomImage().copy(thumbUrl = "${randomUrl()}/$noImage1")))

                search() should have1SearchResultWhere { thumbnailUrl shouldBe null }
            }

            itShould("consider noImageUrl2 as absent image") {
                givenSearchResults(searchResult().copy(image = randomImage().copy(thumbUrl = "${randomUrl()}/$noImage2")))

                search() should have1SearchResultWhere { thumbnailUrl shouldBe null }
            }

            describe("error cases") {
                itShould("throw IllegalStateException on invalid response status") {
                    givenSearchReturns(GiantBombClient.SearchResponse(GiantBombClient.Status.BadFormat, emptyList()))

                    shouldThrow<IllegalStateException> {
                        search()
                    }
                }

                itShould("throw ClientRequestException on invalid http response status") {
                    server.anySearchRequest() willFailWith HttpStatusCode.BadRequest

                    val e = shouldThrow<ClientRequestException> {
                        search()
                    }
                    e.response.status shouldBe HttpStatusCode.BadRequest
                }
            }
        }

        describe("fetch") {
            itShould("fetch details") {
                val result = detailsResult()
                givenFetchResult(result, apiUrl = apiDetailsUrl)

                fetch("${server.baseUrl}/$apiDetailsUrl") shouldBe GameProvider.FetchResponse(
                    gameData = GameData(
                        name = result.name,
                        description = result.deck,
                        releaseDate = result.originalReleaseDate?.toString(),
                        criticScore = null,
                        userScore = null,
                        genres = listOf(result.genres!!.first().name),
                        thumbnailUrl = result.image!!.thumbUrl,
                        posterUrl = result.image!!.superUrl,
                        screenshotUrls = result.images.map { it.superUrl }
                    ),
                    siteUrl = result.siteDetailUrl
                )

                server.verify {
                    getRequestedFor(urlPathEqualTo("/$apiDetailsUrl"))
                        .withQueryParam("api_key", account.apiKey)
                        .withQueryParam(
                            "field_list",
                            "name,deck,original_release_date,expected_release_year," +
                                    "expected_release_quarter,expected_release_month,expected_release_day," +
                                    "image,site_detail_url,genres,images"
                        )
                }
            }

            itShould("return null description when 'deck' is null") {
                givenFetchResult(detailsResult().copy(deck = null))

                fetch().gameData.description shouldBe null
            }

            itShould("return null releaseDate when 'originalReleaseDate' & 'expectedReleaseDates' are null") {
                givenFetchResult(
                    detailsResult().copy(
                        originalReleaseDate = null,
                        expectedReleaseYear = null,
                        expectedReleaseMonth = null,
                        expectedReleaseDay = null,
                        expectedReleaseQuarter = null
                    )
                )

                fetch().gameData.releaseDate shouldBe null
            }

            itShould("use expectedReleaseYear, expectedReleaseMonth & expectedReleaseDay as releaseDate when originalReleaseDate is null and those are present") {
                givenFetchResult(
                    detailsResult().copy(
                        originalReleaseDate = null,
                        expectedReleaseYear = 2019,
                        expectedReleaseMonth = 7,
                        expectedReleaseDay = 12,
                        expectedReleaseQuarter = null
                    )
                )

                fetch().gameData.releaseDate shouldBe "2019-07-12"
            }

            itShould("use expectedReleaseYear & expectedReleaseMonth as releaseDate when originalReleaseDate is null and those are present") {
                givenFetchResult(
                    detailsResult().copy(
                        originalReleaseDate = null,
                        expectedReleaseYear = 1998,
                        expectedReleaseMonth = 10,
                        expectedReleaseDay = null,
                        expectedReleaseQuarter = null
                    )
                )

                fetch().gameData.releaseDate shouldBe "1998-10-01"
            }

            "use expectedReleaseYear & expectedReleaseQuarter as releaseDate when originalReleaseDate is null and those are present, " +
                    itShould("even if expectedReleaseMonth & expectedReleaseDay are also present") {
                        givenFetchResult(
                            detailsResult().copy(
                                originalReleaseDate = null,
                                expectedReleaseYear = 1998,
                                expectedReleaseMonth = 10,
                                expectedReleaseDay = 12,
                                expectedReleaseQuarter = 1
                            )
                        )

                        fetch().gameData.releaseDate shouldBe "1998 Q1"
                    }

            "return null releaseDate if originalReleaseDate & expectedReleaseYear are null," +
                    itShould("even if expectedReleaseMonth & expectedReleaseDay are also present") {
                        givenFetchResult(
                            detailsResult().copy(
                                originalReleaseDate = null,
                                expectedReleaseYear = null,
                                expectedReleaseMonth = 10,
                                expectedReleaseDay = 12,
                                expectedReleaseQuarter = 1
                            )
                        )

                        fetch().gameData.releaseDate shouldBe null
                    }

            itShould("return empty genres when 'genres' field is null") {
                givenFetchResult(detailsResult().copy(genres = null))

                fetch().gameData.genres shouldBe emptyList<String>()
            }

            itShould("return null thumbnail when 'image' is null") {
                givenFetchResult(detailsResult().copy(image = null))

                fetch().gameData.thumbnailUrl shouldBe null
                fetch().gameData.posterUrl shouldBe null
            }

            itShould("consider noImageUrl1 as absent thumbnail") {
                givenFetchResult(detailsResult().copy(image = randomImage().copy(thumbUrl = "${randomUrl()}/$noImage1")))

                fetch().gameData.thumbnailUrl shouldBe null
            }

            itShould("consider noImageUrl2 as absent thumbnail") {
                givenFetchResult(detailsResult().copy(image = randomImage().copy(thumbUrl = "${randomUrl()}/$noImage2")))

                fetch().gameData.thumbnailUrl shouldBe null
            }

            itShould("consider noImageUrl1 as absent poster") {
                givenFetchResult(detailsResult().copy(image = randomImage().copy(superUrl = "${randomUrl()}/$noImage1")))

                fetch().gameData.posterUrl shouldBe null
            }

            itShould("consider noImageUrl2 as absent poster") {
                givenFetchResult(detailsResult().copy(image = randomImage().copy(superUrl = "${randomUrl()}/$noImage2")))

                fetch().gameData.posterUrl shouldBe null
            }

            itShould("filter out no image urls from screenshots") {
                val screenshot1 = randomImage()
                givenFetchResult(
                    detailsResult().copy(
                        images = listOf(
                            randomImage().copy(superUrl = "${randomUrl()}/$noImage1"),
                            screenshot1,
                            randomImage().copy(superUrl = "${randomUrl()}/$noImage2")
                        )
                    )
                )

                fetch().gameData.screenshotUrls shouldBe listOf(screenshot1.superUrl)
            }

            describe("error cases") {
                itShould("throw IllegalStateException on invalid response status") {
                    givenFetchReturns(GiantBombClient.FetchResponse(GiantBombClient.Status.BadFormat, emptyList()))

                    shouldThrow<IllegalStateException> {
                        fetch()
                    }
                }

                itShould("throw ClientRequestException on invalid http response status") {
                    server.aFetchRequest(apiDetailsUrl) willFailWith HttpStatusCode.BadRequest

                    val e = shouldThrow<ClientRequestException> {
                        provider.fetch("${server.baseUrl}/$apiDetailsUrl", platform, account)
                    }
                    e.response.status shouldBe HttpStatusCode.BadRequest
                }
            }
        }
    }

    val server = GiantBombMockServer()
    override fun beforeAll() = server.start()
    override fun afterAll() = server.close()
    override fun beforeEach() = server.reset()

    override fun scope() = Scope()
    inner class Scope {
        val platform = randomEnum<Platform>()
        val platformId = randomInt(100)
        val apiDetailsUrl = randomPath()
        val account = GiantBombUserAccount(apiKey = randomWord())
        val noImage1 = randomWord()
        val noImage2 = randomWord()
        val offset = randomInt(100)
        val limit = randomInt(max = 100, min = 1)

        fun randomImage() = GiantBombClient.Image(thumbUrl = randomUrl(), superUrl = randomUrl())

        fun searchResult() = GiantBombClient.SearchResult(
            apiDetailUrl = randomUrl(),
            name = randomName(),
            deck = randomParagraph(),
            originalReleaseDate = randomLocalDate(),
            expectedReleaseYear = randomInt(min = 1980, max = 2050),
            expectedReleaseQuarter = randomInt(min = 1, max = 4),
            expectedReleaseMonth = randomInt(min = 1, max = 12),
            expectedReleaseDay = randomInt(min = 1, max = 28),
            image = randomImage()
        )

        fun detailsResult() = GiantBombClient.FetchResult(
            siteDetailUrl = randomUrl(),
            name = randomName(),
            deck = randomParagraph(),
            originalReleaseDate = randomLocalDate(),
            expectedReleaseYear = randomInt(min = 1980, max = 2050),
            expectedReleaseQuarter = randomInt(min = 1, max = 4),
            expectedReleaseMonth = randomInt(min = 1, max = 12),
            expectedReleaseDay = randomInt(min = 1, max = 28),
            image = randomImage(),
            images = listOf(randomImage(), randomImage()),
            genres = listOf(GiantBombClient.Genre(randomWord()))
        )

        fun givenSearchResults(vararg results: GiantBombClient.SearchResult) =
            givenSearchReturns(GiantBombClient.SearchResponse(GiantBombClient.Status.OK, results.toList()))

        fun givenSearchReturns(response: GiantBombClient.SearchResponse) =
            server.anySearchRequest() willReturn response

        fun givenFetchResult(result: GiantBombClient.FetchResult, apiUrl: String = apiDetailsUrl) =
            givenFetchReturns(GiantBombClient.FetchResponse(GiantBombClient.Status.OK, listOf(result)), apiUrl)

        fun givenFetchReturns(response: GiantBombClient.FetchResponse, apiUrl: String = apiDetailsUrl) =
            server.aFetchRequest(apiUrl) willReturn response

        suspend fun search(query: String = randomName()) = provider.search(query, platform, account, offset, limit).results
        suspend fun fetch(apiUrl: String = "${server.baseUrl}/$apiDetailsUrl", platform: Platform = this.platform) =
            provider.fetch(apiUrl, platform, account)

        private val config = GiantBombConfig(
            baseUrl = server.baseUrl,
            noImageFileNames = listOf(noImage1, noImage2),
            accountUrl = "",
            defaultOrder = GameProvider.OrderPriorities.default,
            platforms = mapOf(platform.name to platformId)
        )
        val provider = GiantBombProvider(config, GiantBombClient(config))
    }
}
