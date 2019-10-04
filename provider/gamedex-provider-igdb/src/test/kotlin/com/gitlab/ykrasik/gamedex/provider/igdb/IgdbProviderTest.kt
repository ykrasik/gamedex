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

package com.gitlab.ykrasik.gamedex.provider.igdb

import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.gitlab.ykrasik.gamedex.GameData
import com.gitlab.ykrasik.gamedex.Platform
import com.gitlab.ykrasik.gamedex.Score
import com.gitlab.ykrasik.gamedex.provider.GameProvider
import com.gitlab.ykrasik.gamedex.test.*
import io.kotlintest.matchers.*
import io.ktor.client.features.ClientRequestException
import io.ktor.http.HttpStatusCode

/**
 * User: ykrasik
 * Date: 16/04/2017
 * Time: 15:41
 */
class IgdbProviderTest : Spec<IgdbProviderTest.Scope>() {
    init {
        "search" should {
            "be able to return a single search result" test {
                val query = "query"
                val result = searchResult(releaseDate = releaseDate)
                givenSearchResults(result)

                provider.search(query, platform, account, offset, limit) shouldBe GameProvider.SearchResponse(
                    results = listOf(
                        GameProvider.SearchResult(
                            providerGameId = result.id.toString(),
                            name = result.name,
                            description = result.summary,
                            releaseDate = releaseDate,
                            criticScore = Score(result.aggregatedRating!!, result.aggregatedRatingCount!!),
                            userScore = Score(result.rating!!, result.ratingCount!!),
                            thumbnailUrl = thumbnailUrl(result.cover!!.imageId!!)
                        )
                    ),
                    canShowMoreResults = null
                )

                server.verify {
                    postRequestedFor(urlPathEqualTo("/"))
                        .withHeader("Accept", "application/json")
                        .withHeader("user-key", account.apiKey)
                        .withBody(
                            """
                                search "$query";
                                fields name,summary,aggregated_rating,aggregated_rating_count,rating,rating_count,release_dates.category,release_dates.human,release_dates.platform,cover.image_id;
                                where name ~ *"$query"* & release_dates.platform = $platformId;
                                offset $offset;
                                limit $limit;
                            """.trimIndent()
                        )
                }
            }

            "split search name by whitespace & remove non-word characters" test {
                givenSearchResults(searchResult())

                search("name with\tspace & other-characters: yes\nno,   maybe")

                server.verify {
                    postRequestedFor(urlPathEqualTo("/"))
                        .withBodyContaining(
                            """
                                where name ~ *"name"* & name ~ *"with"* & name ~ *"space"* & name ~ *"other"* & name ~ *"characters"* & name ~ *"yes"* & name ~ *"no"* & name ~ *"maybe"*
                            """.trimIndent()
                        )
                }
            }

            "be able to return empty search results" test {
                givenSearchResults()

                search() shouldBe emptyList<GameProvider.SearchResult>()
            }

            "be able to return multiple search results" test {
                val result1 = searchResult()
                val result2 = searchResult()
                givenSearchResults(result1, result2)

                search() should have2SearchResultsWhere { first, second ->
                    first.name shouldBe result1.name
                    second.name shouldBe result2.name
                }
            }

            "return null description when 'summary' is null" test {
                givenSearchResults(searchResult().copy(summary = null))

                search() should have1SearchResultWhere { description shouldBe null }
            }

            "return null criticScore when 'aggregatedRating' is null, even if 'aggregatedRatingCount' isn't" test {
                givenSearchResults(searchResult().copy(aggregatedRating = null, aggregatedRatingCount = 1))

                search() should have1SearchResultWhere { criticScore shouldBe null }
            }

            "return null criticScore when 'aggregatedRating' is not null but 'aggregatedRatingCount' is 0" test {
                givenSearchResults(searchResult().copy(aggregatedRating = 99.9, aggregatedRatingCount = 0))

                search() should have1SearchResultWhere { criticScore shouldBe null }
            }

            "return null userScore when 'rating' is null, event if 'ratingCount' isn't" test {
                givenSearchResults(searchResult().copy(rating = null, ratingCount = 1))

                search() should have1SearchResultWhere { userScore shouldBe null }
            }

            "return null userScore when 'rating' is not null but 'ratingCount' is 0" test {
                givenSearchResults(searchResult().copy(rating = 99.9, ratingCount = 0))

                search() should have1SearchResultWhere { userScore shouldBe null }
            }

            "return null releaseDate when 'releaseDates' is null" test {
                givenSearchResults(searchResult().copy(releaseDates = null))

                search() should have1SearchResultWhere { releaseDate shouldBe null }
            }

            "return null releaseDate when no releaseDate exists for given platform" test {
                givenSearchResults(searchResult(releaseDatePlatformId = platformId + 1))

                search() should have1SearchResultWhere { releaseDate shouldBe null }
            }

            "parse a release date of format YYYY-MMM-dd and return YYYY-MM-dd instead" test {
                givenSearchResults(searchResult(releaseDate = "2000-Apr-07"))

                search() should have1SearchResultWhere { releaseDate shouldBe "2000-04-07" }
            }

            "fallback to returning the original release date when parsing as YYYY-MMM-dd fails" test {
                givenSearchResults(searchResult(releaseDate = "2017-Q4"))

                search() should have1SearchResultWhere { releaseDate shouldBe "2017-Q4" }
            }

            "return null thumbnailUrl when 'cover.cloudinaryId' is null" test {
                givenSearchResults(searchResult().copy(cover = image(imageId = null)))

                search() should have1SearchResultWhere { thumbnailUrl shouldBe null }
            }

            "return null thumbnailUrl when 'cover' is null" test {
                givenSearchResults(searchResult().copy(cover = null))

                search() should have1SearchResultWhere { thumbnailUrl shouldBe null }
            }

            "error cases" should {
                "throw ClientRequestException on invalid http response status" test {
                    server.anyRequest() willFailWith HttpStatusCode.BadRequest

                    val e = shouldThrow<ClientRequestException> {
                        search()
                    }
                    e.message!! shouldHave substring(HttpStatusCode.BadRequest.value.toString())
                }
            }
        }

        "fetch" should {
            "fetch details" test {
                val id = "gameId"
                val result = fetchResult(releaseDate = releaseDate)
                givenFetchResult(result)

                fetch(id) shouldBe GameProvider.FetchResponse(
                    gameData = GameData(
                        name = result.name,
                        description = result.summary,
                        releaseDate = releaseDate,
                        criticScore = Score(result.aggregatedRating!!, result.aggregatedRatingCount!!),
                        userScore = Score(result.rating!!, result.ratingCount!!),
                        genres = listOf(genre),
                        thumbnailUrl = thumbnailUrl(result.cover!!.imageId!!),
                        posterUrl = posterUrl(result.cover!!.imageId!!),
                        screenshotUrls = result.screenshots!!.map { screenshotUrl(it.imageId!!) }
                    ),
                    siteUrl = result.url
                )

                server.verify {
                    postRequestedFor(urlPathEqualTo("/"))
                        .withHeader("Accept", "application/json")
                        .withHeader("user-key", account.apiKey)
                        .withBody(
                            """
                                fields name,summary,aggregated_rating,aggregated_rating_count,rating,rating_count,release_dates.category,release_dates.human,release_dates.platform,cover.image_id,url,screenshots.image_id,genres;
                                where id = $id;
                            """.trimIndent()
                        )
                }
            }

            "return null description when 'summary' is null" test {
                givenFetchResult(fetchResult().copy(summary = null))

                fetch().gameData.description shouldBe null
            }

            "return null criticScore when 'aggregatedRating' is null, even if 'aggregatedRatingCount' isn't" test {
                givenFetchResult(fetchResult().copy(aggregatedRating = null, aggregatedRatingCount = 1))

                fetch().gameData.criticScore shouldBe null
            }

            "return null criticScore when 'aggregatedRating' is not null but 'aggregatedRatingCount' is 0" test {
                givenFetchResult(fetchResult().copy(aggregatedRating = 99.9, aggregatedRatingCount = 0))

                fetch().gameData.criticScore shouldBe null
            }

            "return null userScore when 'rating' is null, event if 'ratingCount' isn't" test {
                givenFetchResult(fetchResult().copy(rating = null, ratingCount = 1))

                fetch().gameData.userScore shouldBe null
            }

            "return null userScore when 'rating' is not null but 'ratingCount' is 0" test {
                givenFetchResult(fetchResult().copy(rating = 99.9, ratingCount = 0))

                fetch().gameData.userScore shouldBe null
            }

            "return null releaseDate when 'releaseDates' is null" test {
                givenFetchResult(fetchResult().copy(releaseDates = null))

                fetch().gameData.releaseDate shouldBe null
            }

            "return null releaseDate when no releaseDate exists for given platform" test {
                givenFetchResult(fetchResult(releaseDatePlatformId = platformId + 1))

                fetch().gameData.releaseDate shouldBe null
            }

            "parse a release date of format YYYY-MMM-dd and return YYYY-MM-dd instead" test {
                givenFetchResult(fetchResult(releaseDate = "2000-Apr-07"))

                fetch().gameData.releaseDate shouldBe "2000-04-07"
            }

            "fallback to returning the original release date when parsing as YYYY-MMM-dd fails" test {
                givenFetchResult(fetchResult(releaseDate = "2017-Q4"))

                fetch().gameData.releaseDate shouldBe "2017-Q4"
            }

            "return empty genres when 'genres' is null" test {
                givenFetchResult(fetchResult().copy(genres = null))

                fetch().gameData.genres shouldBe emptyList<String>()
            }

            "return null thumbnailUrl & posterUrl when 'cover.cloudinaryId' is null" test {
                givenFetchResult(fetchResult().copy(cover = image(imageId = null)))

                fetch().gameData.thumbnailUrl shouldBe null
                fetch().gameData.posterUrl shouldBe null
            }

            "return null thumbnailUrl & posterUrl when 'cover' is null" test {
                givenFetchResult(fetchResult().copy(cover = null))

                fetch().gameData.thumbnailUrl shouldBe null
                fetch().gameData.posterUrl shouldBe null
            }

            "filter out screenshots with null 'imageId'" test {
                val image = image()
                givenFetchResult(fetchResult().copy(screenshots = listOf(image(imageId = null), image)))

                fetch().gameData.screenshotUrls shouldBe listOf(screenshotUrl(image.imageId!!))
            }

            "return empty screenshots when 'screenshots' is null" test {
                givenFetchResult(fetchResult().copy(screenshots = null))

                fetch().gameData.screenshotUrls shouldBe emptyList<String>()
            }

            "error cases" should {
                "throw IllegalStateException on empty response" test {
                    val id = "gameId"
                    server.anyRequest() willReturnFetch emptyList()

                    val e = shouldThrow<IllegalStateException> {
                        fetch(id)
                    }
                    e.message shouldBe "Not found: IGDB game '$id'!"
                }

                "throw ClientRequestException on invalid http response status" test {
                    server.anyRequest() willFailWith HttpStatusCode.BadRequest

                    val e = shouldThrow<ClientRequestException> {
                        fetch()
                    }
                    e.response.status shouldBe HttpStatusCode.BadRequest
                }
            }
        }
    }

    val server = IgdbMockServer()
    override fun beforeAll() = server.start()
    override fun afterAll() = server.close()
    override fun beforeEach() = server.reset()

    override fun scope() = Scope()
    inner class Scope {
        val platform = randomEnum<Platform>()
        val platformId = randomInt(100)
        val genreId = randomInt(100)
        val genre = randomWord()
        val releaseDate = randomLocalDateString()
        val account = IgdbUserAccount(apiKey = randomWord())
        val offset = randomInt(100)
        val limit = randomInt(max = 100, min = 1)

        val baseImageUrl = randomUrl()

        fun thumbnailUrl(imageId: String) = "$baseImageUrl/t_thumb_2x/$imageId.jpg"
        fun posterUrl(imageId: String) = "$baseImageUrl/t_screenshot_huge/$imageId.jpg"
        fun screenshotUrl(imageId: String) = posterUrl(imageId)

        fun searchResult(
            releaseDate: String = randomLocalDateString(),
            releaseDatePlatformId: Int = this.platformId
        ) = IgdbClient.SearchResult(
            id = randomInt(),
            name = randomName(),
            summary = randomParagraph(),
            aggregatedRating = randomScore().score,
            aggregatedRatingCount = randomScore().numReviews,
            rating = randomScore().score,
            ratingCount = randomScore().numReviews,
            releaseDates = listOf(releaseDate(releaseDate, releaseDatePlatformId)),
            cover = image()
        )

        fun fetchResult(
            releaseDate: String = randomLocalDateString(),
            releaseDatePlatformId: Int = this.platformId
        ) = IgdbClient.FetchResult(
            url = randomWord(),
            name = randomName(),
            summary = randomParagraph(),
            releaseDates = listOf(releaseDate(releaseDate, releaseDatePlatformId)),
            aggregatedRating = randomScore().score,
            aggregatedRatingCount = randomScore().numReviews,
            rating = randomScore().score,
            ratingCount = randomScore().numReviews,
            cover = image(),
            screenshots = listOf(image(), image()),
            genres = listOf(genreId)
        )

        private fun releaseDate(releaseDate: String, platformId: Int) = IgdbClient.ReleaseDate(
            platform = platformId,
            category = 0,
            human = releaseDate
        )

        fun image(imageId: String? = randomString()) = IgdbClient.Image(imageId = imageId)

        fun givenSearchResults(vararg results: IgdbClient.SearchResult) =
            server.anyRequest().willReturnSearch(results.toList())

        fun givenFetchResult(result: IgdbClient.FetchResult) =
            server.anyRequest().willReturnFetch(listOf(result))

        suspend fun search(query: String = randomName()) = provider.search(query, platform, account, offset, limit).results
        suspend fun fetch(providerGameId: String = randomString()) = provider.fetch(providerGameId, platform, account)

        private val config = IgdbConfig(
            baseUrl = server.baseUrl,
            baseImageUrl = baseImageUrl,
            accountUrl = "",
            thumbnailImageType = IgdbProvider.IgdbImageType.thumb_2x,
            posterImageType = IgdbProvider.IgdbImageType.screenshot_huge,
            screenshotImageType = IgdbProvider.IgdbImageType.screenshot_huge,
            defaultOrder = GameProvider.OrderPriorities.default,
            platforms = mapOf(platform.name to platformId),
            genres = mapOf(genreId.toString() to genre)
        )
        val provider = IgdbProvider(config, IgdbClient(config))
    }
}