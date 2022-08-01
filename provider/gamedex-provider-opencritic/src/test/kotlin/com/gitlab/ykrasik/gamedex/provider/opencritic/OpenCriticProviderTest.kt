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

package com.gitlab.ykrasik.gamedex.provider.opencritic

import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.gitlab.ykrasik.gamedex.GameData
import com.gitlab.ykrasik.gamedex.Platform
import com.gitlab.ykrasik.gamedex.Score
import com.gitlab.ykrasik.gamedex.provider.GameProvider
import com.gitlab.ykrasik.gamedex.test.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.haveSize
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.haveSubstring
import io.ktor.client.plugins.*
import io.ktor.http.*
import org.joda.time.DateTime
import kotlin.random.Random

/**
 * User: ykrasik
 * Date: 28/09/2019
 * Time: 22:15
 */
class OpenCriticProviderTest : Spec<OpenCriticProviderTest.Scope>() {
    init {
        describe("search") {
            itShould("be able to return a single search result") {
                val result = searchResult()
                server.anySearchRequest().willReturn(listOf(result))
                givenEmptyFetchResultFor(result)

                provider.search(query, Platform.Windows, GameProvider.Account.Null, offset, limit) shouldBe GameProvider.SearchResponse(
                    results = listOf(
                        GameProvider.SearchResult(
                            providerGameId = result.id.toString(),
                            name = result.name,
                            description = null,
                            releaseDate = null,
                            criticScore = null,
                            userScore = null,
                            thumbnailUrl = null
                        )
                    ),
                    canShowMoreResults = false
                )

                server.verify {
                    getRequestedFor(urlPathEqualTo("/api/meta/search"))
                        .withQueryParam("criteria", query)
                }
            }

            itShould("be able to return empty search results") {
                server.anySearchRequest().willReturn(emptyList())

                search() shouldBe emptyList()
            }

            itShould("be able to return multiple search results") {
                val result1 = searchResult()
                val result2 = searchResult()
                server.anySearchRequest().willReturn(listOf(result1, result2))
                givenEmptyFetchResultFor(result1)

                val results = search()
                results should haveSize(2)
                results[0].name shouldBe result1.name
                results[1].name shouldBe result2.name
            }

            itShould("pre-fetch first search result") {
                val result1 = searchResult()
                val result2 = searchResult()
                server.anySearchRequest().willReturn(listOf(result1, result2))

                val fetchResult = fetchResult(releaseDate = releaseDate).copy(id = result1.id)
                givenFetchReturns(fetchResult)

                search() shouldBe listOf(
                    GameProvider.SearchResult(
                        providerGameId = result1.id.toString(),
                        name = fetchResult.name,
                        description = fetchResult.description,
                        releaseDate = releaseDate.toLocalDate().toString(),
                        criticScore = Score(fetchResult.medianScore, fetchResult.numReviews),
                        userScore = null,
                        thumbnailUrl = "http:${fetchResult.verticalLogoScreenshot!!.fullRes}"
                    ),
                    GameProvider.SearchResult(
                        providerGameId = result2.id.toString(),
                        name = result2.name,
                        description = null,
                        releaseDate = null,
                        criticScore = null,
                        userScore = null,
                        thumbnailUrl = null
                    )
                )

                server.verify {
                    getRequestedFor(urlPathEqualTo("/api/game/${result1.id}"))
                }
            }

            describe("error cases") {
                itShould("throw ClientRequestException on invalid http response status") {
                    server.anySearchRequest() willFailWith HttpStatusCode.BadRequest

                    val e = shouldThrow<ClientRequestException> {
                        search()
                    }
                    e.message should haveSubstring(HttpStatusCode.BadRequest.value.toString())
                }
            }
        }

        describe("fetch") {
            itShould("fetch details") {
                val result = fetchResult(releaseDate = releaseDate).copy(name = randomWord(), medianScore = 87.6, numReviews = 2)
                givenFetchReturns(result)

                fetch(result.id) shouldBe GameProvider.FetchResponse(
                    gameData = GameData(
                        name = result.name,
                        description = result.description,
                        releaseDate = releaseDate.toLocalDate().toString(),
                        criticScore = Score(87.6, 2),
                        userScore = null,
                        genres = result.Genres.map { it.name },
                        thumbnailUrl = "http:${result.verticalLogoScreenshot!!.fullRes}",
                        posterUrl = null,
                        screenshotUrls = listOf(screenshotUrl(result.mastheadScreenshot!!)) + result.screenshots.map(::screenshotUrl)
                    ),
                    siteUrl = "${server.baseUrl}/game/${result.id}/${result.name.lowercase()}"
                )

                server.verify {
                    getRequestedFor(urlPathEqualTo("/api/game/${result.id}"))
                }
            }

            itShould("return null description if 'description' is null") {
                givenFetchReturns(fetchResult().copy(description = null))

                fetch().gameData.description shouldBe null
            }

            itShould("return null description if 'description' is empty") {
                givenFetchReturns(fetchResult().copy(description = ""))

                fetch().gameData.description shouldBe null
            }

            itShould("return null criticScore if 'averageScore' is -1") {
                givenFetchReturns(fetchResult().copy(medianScore = -1.0))

                fetch().gameData.criticScore shouldBe null
            }

            itShould("return null criticScore if 'numReviews' is 0") {
                givenFetchReturns(fetchResult().copy(numReviews = 0))

                fetch().gameData.criticScore shouldBe null
            }

            itShould("return releaseDate from 'Platforms'") {
                givenFetchReturns(
                    fetchResult().copy(
                        Platforms = listOf(
                            platform(),
                            platform(id = platformId, releaseDate = DateTime.parse("1999-08-07T11:23:14")),
                            platform()
                        ),
                        firstReleaseDate = randomDateTime()
                    )
                )

                fetch().gameData.releaseDate shouldBe "1999-08-07"
            }

            itShould("return releaseDate from 'firstReleaseDate' when platform not found in 'Platforms'") {
                givenFetchReturns(
                    fetchResult().copy(
                        Platforms = listOf(
                            platform(),
                            platform()
                        ),
                        firstReleaseDate = DateTime.parse("2019-12-12T23:58:59")
                    )
                )

                fetch().gameData.releaseDate shouldBe "2019-12-12"
            }

            itShould("return releaseDate from 'firstReleaseDate' when platform is found in 'Platforms', but releaseDate is null") {
                givenFetchReturns(
                    fetchResult().copy(
                        Platforms = listOf(
                            platform(id = platformId, releaseDate = null),
                            platform()
                        ),
                        firstReleaseDate = DateTime.parse("2019-12-12T23:58:59")
                    )
                )

                fetch().gameData.releaseDate shouldBe "2019-12-12"
            }

            itShould("return null releaseDate when platform not found in 'Platforms' and 'firstReleaseDate' is null") {
                givenFetchReturns(
                    fetchResult().copy(
                        Platforms = listOf(
                            platform(),
                            platform()
                        ),
                        firstReleaseDate = null
                    )
                )

                fetch().gameData.releaseDate shouldBe null
            }

            // TODO: Fix these at some point.
//            itShould("return null thumbnail when 'logoScreenshot' is null") {
//                givenFetchReturns(fetchResult().copy(logoScreenshot = image().copy(thumbnail = null)))
//
//                fetch().gameData.thumbnailUrl shouldBe null
//            }
//
//            itShould("return null thumbnail when 'logoScreenshot.thumbnail' is null") {
//                givenFetchReturns(fetchResult().copy(logoScreenshot = null))
//
//                fetch().gameData.thumbnailUrl shouldBe null
//            }

            itShould("return screenshots only from 'mastheadScreenshot' when 'screenshots' is empty") {
                val result = fetchResult().copy(screenshots = emptyList())
                givenFetchReturns(result)

                fetch().gameData.screenshotUrls shouldBe listOf(screenshotUrl(result.mastheadScreenshot!!))
            }

            itShould("return screenshots only from 'screenshots' when 'mastheadScreenshot' is null") {
                val result = fetchResult().copy(mastheadScreenshot = null, screenshots = listOf(image(), image()))
                givenFetchReturns(result)

                fetch().gameData.screenshotUrls shouldBe listOf(
                    screenshotUrl(result.screenshots[0]),
                    screenshotUrl(result.screenshots[1])
                )
            }

            itShould("return screenshots from 'mastheadScreenshot' and 'screenshots' when both are available") {
                val result = fetchResult().copy(mastheadScreenshot = image(), screenshots = listOf(image(), image()))
                givenFetchReturns(result)

                fetch().gameData.screenshotUrls shouldBe listOf(
                    screenshotUrl(result.mastheadScreenshot!!),
                    screenshotUrl(result.screenshots[0]),
                    screenshotUrl(result.screenshots[1])
                )
            }

            itShould("return siteUrl as the game name lowercased with all non-word characters replaced by '-'") {
                givenFetchReturns(fetchResult().copy(name = "My Test: GAME naME!# yes."))

                fetch().siteUrl shouldBe "${server.baseUrl}/game/$providerGameId/my-test-game-name-yes-"
            }

            describe("error cases") {
                itShould("throw ClientRequestException on invalid http response status") {
                    server.aFetchRequest(providerGameId) willFailWith HttpStatusCode.BadRequest

                    val e = shouldThrow<ClientRequestException> {
                        fetch(providerGameId)
                    }
                    e.response.status shouldBe HttpStatusCode.BadRequest
                }
            }
        }
    }

    val server = OpenCriticMockServer()
    override fun beforeAll() = server.start()
    override fun afterAll() = server.close()
    override fun beforeEach() = server.reset()

    override fun scope() = Scope()
    inner class Scope {
        val platformId = randomInt(100)
        val query = randomWord()
        val providerGameId = randomInt()
        val releaseDate = randomDateTime()
        val offset = randomInt(100)
        val limit = randomInt(max = 100, min = 1)

        val baseUrl = server.baseUrl

        fun searchResult() = OpenCriticClient.SearchResult(
            id = randomInt(),
            name = randomWord(),
            dist = Random.nextDouble(),
            relation = "game"
        )

        fun fetchResult(releaseDate: DateTime = randomDateTime()) = OpenCriticClient.FetchResult(
            id = providerGameId,
            name = randomName(),
            description = randomParagraph(),
            medianScore = randomScore().score,
            numReviews = randomScore().numReviews,
            Genres = listOf(genre(), genre()),
            Platforms = listOf(platform(id = platformId, releaseDate = releaseDate), platform()),
            firstReleaseDate = releaseDate,
            verticalLogoScreenshot = image(),
            bannerScreenshot = image(),
            logoScreenshot = image(),
            mastheadScreenshot = image(),
            screenshots = listOf(image(), image())
        )

        fun genre(genre: String = randomGenre()) = OpenCriticClient.Genre(id = randomInt(), name = genre)

        fun platform(id: Int = randomInt(), releaseDate: DateTime? = randomDateTime()) =
            OpenCriticClient.Platform(id = id, shortName = randomWord(), releaseDate = releaseDate)

        fun image(fullResPath: String = randomPath(), thumbnailPath: String = randomPath()) =
            OpenCriticClient.Image(fullRes = "//$fullResPath", thumbnail = "//$thumbnailPath")

        fun screenshotUrl(image: OpenCriticClient.Image) = "http:${image.fullRes}"

        fun givenFetchReturns(result: OpenCriticClient.FetchResult) =
            server.aFetchRequest(result.id).willReturn(result)

        fun givenEmptyFetchResultFor(result: OpenCriticClient.SearchResult) =
            givenFetchReturns(emptyFetchResultFor(result))

        fun emptyFetchResultFor(result: OpenCriticClient.SearchResult) = OpenCriticClient.FetchResult(
            id = result.id,
            name = result.name,
            description = "",
            medianScore = 0.0,
            numReviews = 0,
            Genres = emptyList(),
            Platforms = emptyList(),
            firstReleaseDate = null,
            verticalLogoScreenshot = null,
            bannerScreenshot = null,
            logoScreenshot = null,
            mastheadScreenshot = null,
            screenshots = emptyList()
        )

        suspend fun search(query: String = randomName()) =
            provider.search(query, Platform.Windows, GameProvider.Account.Null, offset, limit).results

        suspend fun fetch(providerGameId: Int = this.providerGameId) =
            provider.fetch(providerGameId.toString(), Platform.Windows, GameProvider.Account.Null)

        val config = OpenCriticConfig(
            baseUrl = baseUrl,
            defaultOrder = GameProvider.OrderPriorities.default,
            platforms = mapOf(Platform.Windows.name to platformId)
        )
        val provider = OpenCriticProvider(config, OpenCriticClient(config))
    }
}
