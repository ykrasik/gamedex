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

package com.gitlab.ykrasik.gamedex.provider.opencritic

import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.gitlab.ykrasik.gamedex.GameData
import com.gitlab.ykrasik.gamedex.Platform
import com.gitlab.ykrasik.gamedex.Score
import com.gitlab.ykrasik.gamedex.provider.GameProvider
import com.gitlab.ykrasik.gamedex.test.*
import io.kotlintest.matchers.*
import io.ktor.client.features.ClientRequestException
import io.ktor.http.HttpStatusCode
import org.joda.time.DateTime
import kotlin.random.Random

/**
 * User: ykrasik
 * Date: 28/09/2019
 * Time: 22:15
 */
class OpenCriticProviderTest : Spec<OpenCriticProviderTest.Scope>() {
    init {
        "search" should {
            "be able to return a single search result" test {
                val result = searchResult()
                server.anySearchRequest().willReturn(listOf(result))

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
                    getRequestedFor(urlPathEqualTo("/api/game/search"))
                        .withQueryParam("criteria", query)
                }
            }

            "be able to return empty search results" test {
                server.anySearchRequest().willReturn(emptyList())

                search() shouldBe emptyList<GameProvider.SearchResult>()
            }

            "be able to return multiple search results" test {
                val result1 = searchResult()
                val result2 = searchResult()
                server.anySearchRequest().willReturn(listOf(result1, result2))

                val results = search()
                results should haveSize(2)
                results[0].name shouldBe result1.name
                results[1].name shouldBe result2.name
            }

            "error cases" should {
                "throw ClientRequestException on invalid http response status" test {
                    server.anySearchRequest() willFailWith HttpStatusCode.BadRequest

                    val e = shouldThrow<ClientRequestException> {
                        search()
                    }
                    e.message!! shouldHave substring(HttpStatusCode.BadRequest.value.toString())
                }
            }
        }

        "fetch" should {
            "fetch details" test {
                val result = fetchResult(releaseDate = releaseDate).copy(name = randomWord())
                givenFetchReturns(result, result.id)

                fetch(result.id) shouldBe GameProvider.FetchResponse(
                    gameData = GameData(
                        name = result.name,
                        description = result.description,
                        releaseDate = releaseDate.toLocalDate().toString(),
                        criticScore = Score(result.averageScore, result.numReviews),
                        userScore = null,
                        genres = result.Genres.map { it.name },
                        thumbnailUrl = "http:${result.logoScreenshot!!.thumbnail}",
                        posterUrl = null,
                        screenshotUrls = listOf(screenshotUrl(result.mastheadScreenshot!!)) + result.screenshots.map(::screenshotUrl)
                    ),
                    siteUrl = "${server.baseUrl}/game/${result.id}/${result.name.toLowerCase()}"
                )

                server.verify {
                    getRequestedFor(urlPathEqualTo("/api/game/${result.id}"))
                }
            }

            "return null criticScore if 'averageScore' field is -1" test {
                givenFetchReturns(fetchResult().copy(averageScore = -1.0))

                fetch().gameData.criticScore shouldBe null
            }

            "return null criticScore if 'numReviews' field is 0" test {
                givenFetchReturns(fetchResult().copy(numReviews = 0))

                fetch().gameData.criticScore shouldBe null
            }

            "return releaseDate from the 'Platforms' field" test {
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

            "return releaseDate from 'firstReleaseDate' field when platform not found in 'Platforms' field" test {
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

            "return null releaseDate when platform not found in 'Platforms' and 'firstReleaseDate' is null" test {
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

            "return null thumbnail when 'logoScreenshot' field is null" test {
                givenFetchReturns(fetchResult().copy(logoScreenshot = null))

                fetch().gameData.thumbnailUrl shouldBe null
            }

            "return screenshots only from 'mastheadScreenshot' field when the 'screenshots' field is empty" test {
                val result = fetchResult().copy(screenshots = emptyList())
                givenFetchReturns(result)

                fetch().gameData.screenshotUrls shouldBe listOf(screenshotUrl(result.mastheadScreenshot!!))
            }

            "return screenshots only from 'screenshots' field when the 'mastheadScreenshot' field is null" test {
                val result = fetchResult().copy(mastheadScreenshot = null, screenshots = listOf(image(), image()))
                givenFetchReturns(result)

                fetch().gameData.screenshotUrls shouldBe listOf(
                    screenshotUrl(result.screenshots[0]),
                    screenshotUrl(result.screenshots[1])
                )
            }

            "return screenshots from 'mastheadScreenshot' and 'mastheadScreenshot' fields when both are available" test {
                val result = fetchResult().copy(mastheadScreenshot = image(), screenshots = listOf(image(), image()))
                givenFetchReturns(result)

                fetch().gameData.screenshotUrls shouldBe listOf(
                    screenshotUrl(result.mastheadScreenshot!!),
                    screenshotUrl(result.screenshots[0]),
                    screenshotUrl(result.screenshots[1])
                )
            }

            "return siteUrl as the game name lowercased with all non-word characters replaced by '-'" test {
                givenFetchReturns(fetchResult().copy(name = "My Test: GAME naME!# yes."))

                fetch().siteUrl shouldBe "${server.baseUrl}/game/$providerGameId/my-test-game-name-yes-"
            }

            "error cases" should {
                "throw ClientRequestException on invalid http response status" test {
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
            dist = Random.nextDouble()
        )

        fun fetchResult(releaseDate: DateTime = randomDateTime()) = OpenCriticClient.FetchResult(
            id = providerGameId,
            name = randomName(),
            description = randomParagraph(),
            averageScore = randomScore().score,
            numReviews = randomScore().numReviews,
            Genres = listOf(genre(), genre()),
            Platforms = listOf(platform(id = platformId, releaseDate = releaseDate), platform()),
            firstReleaseDate = releaseDate,
            logoScreenshot = image(),
            mastheadScreenshot = image(),
            screenshots = listOf(image(), image())
        )

        fun genre(genre: String = randomGenre()) = OpenCriticClient.Genre(id = randomInt(), name = genre)

        fun platform(id: Int = randomInt(), releaseDate: DateTime = randomDateTime()) =
            OpenCriticClient.Platform(id = id, shortName = randomWord(), releaseDate = releaseDate)

        fun image(fullResPath: String = randomPath(), thumbnailPath: String = randomPath()) =
            OpenCriticClient.Image(fullRes = "//$fullResPath", thumbnail = "//$thumbnailPath")

        fun screenshotUrl(image: OpenCriticClient.Image) = "http:${image.fullRes}"

        fun givenFetchReturns(result: OpenCriticClient.FetchResult, providerGameId: Int = this.providerGameId) =
            server.aFetchRequest(providerGameId).willReturn(result)

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