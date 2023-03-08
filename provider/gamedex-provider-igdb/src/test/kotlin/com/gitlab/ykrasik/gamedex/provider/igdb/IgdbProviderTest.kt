/****************************************************************************
 * Copyright (C) 2016-2023 Yevgeny Krasik                                   *
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
import com.gitlab.ykrasik.gamedex.util.JodaLocalDate
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.haveSubstring
import io.ktor.client.plugins.*
import io.ktor.http.*

/**
 * User: ykrasik
 * Date: 16/04/2017
 * Time: 15:41
 */
class IgdbProviderTest : Spec<IgdbProviderTest.Scope>() {
    init {
        describe("search") {
            itShould("be able to return a single search result") {
                val query = "query"
                val result = searchResult(releaseDate = releaseDate)
                givenSearchResults(result)

                provider.search(query, platform, account, offset, limit) shouldBe GameProvider.SearchResponse(
                    results = listOf(
                        GameProvider.SearchResult(
                            providerGameId = result.id.toString(),
                            name = result.name,
                            description = result.summary,
                            releaseDate = releaseDate.toString("YYYY-MM-dd"),
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
                        .withHeader("Client-ID", account.clientId)
                        .withHeader("Authorization", "Bearer $authorizationToken")
                        .withBody(
                            """
                                search "$query";
                                fields name,summary,aggregated_rating,aggregated_rating_count,rating,rating_count,release_dates.date,release_dates.platform,first_release_date,cover.image_id;
                                where name ~ *"$query"* & release_dates.platform = $platformId;
                                offset $offset;
                                limit $limit;
                            """.trimIndent()
                        )
                }
            }

            itShould("split search name by whitespace & remove non-word characters") {
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

            itShould("return null description when 'summary' is null") {
                givenSearchResults(searchResult().copy(summary = null))

                search() should have1SearchResultWhere { description shouldBe null }
            }

            itShould("return null criticScore when 'aggregatedRating' is null, even if 'aggregatedRatingCount' isn't") {
                givenSearchResults(searchResult().copy(aggregatedRating = null, aggregatedRatingCount = 1))

                search() should have1SearchResultWhere { criticScore shouldBe null }
            }

            itShould("return null criticScore when 'aggregatedRating' is not null but 'aggregatedRatingCount' is 0") {
                givenSearchResults(searchResult().copy(aggregatedRating = 99.9, aggregatedRatingCount = 0))

                search() should have1SearchResultWhere { criticScore shouldBe null }
            }

            itShould("return null userScore when 'rating' is null, event if 'ratingCount' isn't") {
                givenSearchResults(searchResult().copy(rating = null, ratingCount = 1))

                search() should have1SearchResultWhere { userScore shouldBe null }
            }

            itShould("return null userScore when 'rating' is not null but 'ratingCount' is 0") {
                givenSearchResults(searchResult().copy(rating = 99.9, ratingCount = 0))

                search() should have1SearchResultWhere { userScore shouldBe null }
            }

            itShould("return null releaseDate when 'releaseDates' is null & 'firstReleaseDate' is null") {
                givenSearchResults(searchResult().copy(releaseDates = null, firstReleaseDate = null))

                search() should have1SearchResultWhere { releaseDate shouldBe null }
            }

            itShould("return null releaseDate when no releaseDate exists for given platform and 'firstReleaseDate' is null") {
                givenSearchResults(searchResult(releaseDatePlatformId = platformId + 1).copy(firstReleaseDate = null))

                search() should have1SearchResultWhere { releaseDate shouldBe null }
            }

            itShould("return 'firstReleaseDate' as releaseDate when no releaseDate exists for given platform") {
                givenSearchResults(searchResult(releaseDatePlatformId = platformId + 1))

                search() should have1SearchResultWhere { releaseDate shouldBe firstReleaseDate.toString("YYYY-MM-dd") }
            }

            itShould("return 'firstReleaseDate' as releaseDate when releaseDate is null for given platform") {
                givenSearchResults(searchResult(releaseDate = null))

                search() should have1SearchResultWhere { releaseDate shouldBe firstReleaseDate.toString("YYYY-MM-dd") }
            }

            itShould("return null thumbnailUrl when 'cover.cloudinaryId' is null") {
                givenSearchResults(searchResult().copy(cover = image(imageId = null)))

                search() should have1SearchResultWhere { thumbnailUrl shouldBe null }
            }

            itShould("return null thumbnailUrl when 'cover' is null") {
                givenSearchResults(searchResult().copy(cover = null))

                search() should have1SearchResultWhere { thumbnailUrl shouldBe null }
            }

            describe("error cases") {
                itShould("throw ClientRequestException on invalid http response status") {
                    server.anyRequest() willFailWith HttpStatusCode.BadRequest

                    val e = shouldThrow<ClientRequestException> {
                        search()
                    }
                    e.message should haveSubstring(HttpStatusCode.BadRequest.value.toString())
                }
            }
        }

        describe("fetch") {
            itShould("fetch details") {
                val id = "gameId"
                val result = fetchResult(releaseDate = releaseDate)
                givenFetchResult(result)

                fetch(id) shouldBe GameProvider.FetchResponse(
                    gameData = GameData(
                        name = result.name,
                        description = result.summary,
                        releaseDate = releaseDate.toString("YYYY-MM-dd"),
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
                        .withHeader("Client-ID", account.clientId)
                        .withHeader("Authorization", "Bearer $authorizationToken")
                        .withBody(
                            """
                                fields name,summary,aggregated_rating,aggregated_rating_count,rating,rating_count,release_dates.date,release_dates.platform,first_release_date,cover.image_id,url,screenshots.image_id,genres;
                                where id = $id;
                            """.trimIndent()
                        )
                }
            }

            itShould("return null description when 'summary' is null") {
                givenFetchResult(fetchResult().copy(summary = null))

                fetch().gameData.description shouldBe null
            }

            itShould("return null criticScore when 'aggregatedRating' is null, even if 'aggregatedRatingCount' isn't") {
                givenFetchResult(fetchResult().copy(aggregatedRating = null, aggregatedRatingCount = 1))

                fetch().gameData.criticScore shouldBe null
            }

            itShould("return null criticScore when 'aggregatedRating' is not null but 'aggregatedRatingCount' is 0") {
                givenFetchResult(fetchResult().copy(aggregatedRating = 99.9, aggregatedRatingCount = 0))

                fetch().gameData.criticScore shouldBe null
            }

            itShould("return null userScore when 'rating' is null, event if 'ratingCount' isn't") {
                givenFetchResult(fetchResult().copy(rating = null, ratingCount = 1))

                fetch().gameData.userScore shouldBe null
            }

            itShould("return null userScore when 'rating' is not null but 'ratingCount' is 0") {
                givenFetchResult(fetchResult().copy(rating = 99.9, ratingCount = 0))

                fetch().gameData.userScore shouldBe null
            }

            itShould("return null releaseDate when 'releaseDates' is null & 'firstReleaseDate' is null") {
                givenFetchResult(fetchResult().copy(releaseDates = null, firstReleaseDate = null))

                fetch().gameData.releaseDate shouldBe null
            }

            itShould("return null releaseDate when no releaseDate exists for given platform and 'firstReleaseDate' is null") {
                givenFetchResult(fetchResult(releaseDatePlatformId = platformId + 1).copy(firstReleaseDate = null))

                fetch().gameData.releaseDate shouldBe null
            }

            itShould("return 'firstReleaseDate' as releaseDate when no releaseDate exists for given platform") {
                givenFetchResult(fetchResult(releaseDatePlatformId = platformId + 1))

                fetch().gameData.releaseDate shouldBe firstReleaseDate.toString("YYYY-MM-dd")
            }

            itShould("return 'firstReleaseDate' as releaseDate when releaseDate is null for given platform") {
                givenFetchResult(fetchResult(releaseDate = null))

                fetch().gameData.releaseDate shouldBe firstReleaseDate.toString("YYYY-MM-dd")
            }

            itShould("return empty genres when 'genres' is null") {
                givenFetchResult(fetchResult().copy(genres = null))

                fetch().gameData.genres shouldBe emptyList<String>()
            }

            itShould("return null thumbnailUrl & posterUrl when 'cover.cloudinaryId' is null") {
                givenFetchResult(fetchResult().copy(cover = image(imageId = null)))

                fetch().gameData.thumbnailUrl shouldBe null
                fetch().gameData.posterUrl shouldBe null
            }

            itShould("return null thumbnailUrl & posterUrl when 'cover' is null") {
                givenFetchResult(fetchResult().copy(cover = null))

                fetch().gameData.thumbnailUrl shouldBe null
                fetch().gameData.posterUrl shouldBe null
            }

            itShould("filter out screenshots with null 'imageId'") {
                val image = image()
                givenFetchResult(fetchResult().copy(screenshots = listOf(image(imageId = null), image)))

                fetch().gameData.screenshotUrls shouldBe listOf(screenshotUrl(image.imageId!!))
            }

            itShould("return empty screenshots when 'screenshots' is null") {
                givenFetchResult(fetchResult().copy(screenshots = null))

                fetch().gameData.screenshotUrls shouldBe emptyList<String>()
            }

            describe("error cases") {
                itShould("throw IllegalStateException on empty response") {
                    val id = "gameId"
                    server.anyRequest() willReturnFetch emptyList()

                    val e = shouldThrow<IllegalStateException> {
                        fetch(id)
                    }
                    e.message shouldBe "Not found: IGDB game '$id'!"
                }

                itShould("throw ClientRequestException on invalid http response status") {
                    server.anyRequest() willFailWith HttpStatusCode.BadRequest

                    val e = shouldThrow<ClientRequestException> {
                        fetch()
                    }
                    e.response.status shouldBe HttpStatusCode.BadRequest
                }
            }
        }

        describe("OAuth token") {
            itShould("exchange clientId & clientSecret for an authorization token if there is no previous authorizationToken in the storage") {
                storage.reset()
                val newAuthorizationToken = randomString()
                server.oauthRequest().willReturn(newAuthorizationToken, 10)

                givenFetchResult(fetchResult())
                fetch()

                verifyAuthorizationTokenRequested()
            }

            itShould("exchange clientId & clientSecret for an authorization token if there is a previous authorizationToken in the storage but it is expired") {
                storage.set(IgdbStorageData(authorizationToken = "oldToken", expiresOn = now))

                val newAuthorizationToken = randomString()
                server.oauthRequest().willReturn(newAuthorizationToken, 10)

                givenFetchResult(fetchResult())
                fetch()

                verifyAuthorizationTokenRequested()
                storage.get()?.authorizationToken shouldBe newAuthorizationToken
            }

            itShould("exchange clientId & clientSecret for an authorization token if there is a previous authorizationToken in the storage but it is almost expired (within expiration buffer time)") {
                storage.set(
                    IgdbStorageData(
                        authorizationToken = "oldToken",
                        expiresOn = now.plusSeconds(oauthTokenExpirationBufferSeconds - 1)
                    )
                )

                val newAuthorizationToken = randomString()
                server.oauthRequest().willReturn(newAuthorizationToken, 10)

                givenFetchResult(fetchResult())
                fetch()

                verifyAuthorizationTokenRequested()
                storage.get()?.authorizationToken shouldBe newAuthorizationToken
            }

            itShould("not renew token if previous token wasn't expired") {
                val authorizationToken = "authorizationToken"
                storage.set(
                    IgdbStorageData(
                        authorizationToken = authorizationToken,
                        expiresOn = now.plusSeconds(oauthTokenExpirationBufferSeconds)
                    )
                )

                givenFetchResult(fetchResult())
                fetch()

                // Check oauth endpoint not called
                shouldThrowAny {
                    server.verify {
                        postRequestedFor(urlPathEqualTo("/${server.oauthPath}"))
                    }
                }
                storage.get()?.authorizationToken shouldBe authorizationToken
            }

            itShould("store returned token in storage with correct expiresAt time") {
                storage.reset()

                val newAuthorizationToken = randomString()
                server.oauthRequest().willReturn(newAuthorizationToken, 100)

                givenFetchResult(fetchResult())
                fetch()

                storage.get()?.authorizationToken shouldBe newAuthorizationToken
                storage.get()?.expiresOn shouldBe now.plusSeconds(100)
            }

            itShould("delete stored auth token on error from provider") {
                val authorizationToken = "authorizationToken"
                storage.set(IgdbStorageData(authorizationToken = authorizationToken, expiresOn = now.plusYears(1)))

                server.anyRequest() willFailWith HttpStatusCode.BadRequest

                shouldThrowAny {
                    fetch()
                }

                storage.get() shouldBe null
            }

            describe("error cases") {
                itShould("throw ClientRequestException on invalid http response status") {
                    storage.reset()
                    server.oauthRequest() willFailWith HttpStatusCode.BadRequest

                    val e = shouldThrow<ClientRequestException> {
                        fetch()
                    }
                    e.response.status shouldBe HttpStatusCode.BadRequest
                }
            }
        }
    }

    val authorizationToken = randomString()
    val server = IgdbMockServer()
    val storage = MockSingleValueStorage(
        IgdbStorageData(
            authorizationToken = authorizationToken,
            expiresOn = now.plusYears(1)
        )
    )

    override fun beforeAll() = server.start()
    override fun afterAll() = server.close()
    override fun beforeEach() {
        server.reset()
        server.oauthRequest().willReturn(authorizationToken, 99999)
    }

    override fun scope() = Scope()
    inner class Scope {
        val platform = randomEnum<Platform>()
        val platformId = randomInt(100)
        val genreId = randomInt(100)
        val genre = randomWord()
        val releaseDate = randomLocalDate()
        val firstReleaseDate = randomLocalDate()
        val account = IgdbUserAccount(clientId = randomString(), clientSecret = randomString())
        val offset = randomInt(100)
        val limit = randomInt(max = 100, min = 1)
        val oauthTokenExpirationBufferSeconds = 10

        val baseImageUrl = randomUrl()

        fun thumbnailUrl(imageId: String) = "$baseImageUrl/t_thumb_2x/$imageId.jpg"
        fun posterUrl(imageId: String) = "$baseImageUrl/t_screenshot_huge/$imageId.jpg"
        fun screenshotUrl(imageId: String) = posterUrl(imageId)

        fun searchResult(
            releaseDate: JodaLocalDate? = this.releaseDate,
            releaseDatePlatformId: Int = this.platformId,
        ) = IgdbClient.SearchResult(
            id = randomInt(),
            name = randomName(),
            summary = randomParagraph(),
            aggregatedRating = randomScore().score,
            aggregatedRatingCount = randomScore().numReviews,
            rating = randomScore().score,
            ratingCount = randomScore().numReviews,
            releaseDates = listOf(releaseDate(releaseDate, releaseDatePlatformId)),
            firstReleaseDate = this.firstReleaseDate.toDateTimeAtStartOfDay().millis.div(1000),
            cover = image()
        )

        fun fetchResult(
            releaseDate: JodaLocalDate? = this.releaseDate,
            releaseDatePlatformId: Int = this.platformId,
        ) = IgdbClient.FetchResult(
            url = randomWord(),
            name = randomName(),
            summary = randomParagraph(),
            releaseDates = listOf(releaseDate(releaseDate, releaseDatePlatformId)),
            firstReleaseDate = this.firstReleaseDate.toDateTimeAtStartOfDay().millis.div(1000),
            aggregatedRating = randomScore().score,
            aggregatedRatingCount = randomScore().numReviews,
            rating = randomScore().score,
            ratingCount = randomScore().numReviews,
            cover = image(),
            screenshots = listOf(image(), image()),
            genres = listOf(genreId)
        )

        private fun releaseDate(releaseDate: JodaLocalDate?, platformId: Int) = IgdbClient.ReleaseDate(
            platform = platformId,
            date = releaseDate?.toDateTimeAtStartOfDay()?.millis?.div(1000)
        )

        fun image(imageId: String? = randomString()) = IgdbClient.Image(imageId = imageId)

        fun givenSearchResults(vararg results: IgdbClient.SearchResult) =
            server.anyRequest().willReturnSearch(results.toList())

        fun givenFetchResult(result: IgdbClient.FetchResult) =
            server.anyRequest().willReturnFetch(listOf(result))

        suspend fun search(query: String = randomName()) = provider.search(query, platform, account, offset, limit).results
        suspend fun fetch(providerGameId: String = randomString()) = provider.fetch(providerGameId, platform, account)

        fun verifyAuthorizationTokenRequested() {
            server.verify {
                postRequestedFor(urlPathEqualTo("/${server.oauthPath}"))
                    .withQueryParam("client_id", account.clientId)
                    .withQueryParam("client_secret", account.clientSecret)
                    .withQueryParam("grant_type", "client_credentials")
            }
        }

        private val config = IgdbConfig(
            baseUrl = server.baseUrl,
            baseImageUrl = baseImageUrl,
            oauthUrl = server.oauthUrl,
            oauthTokenExpirationBufferSeconds = oauthTokenExpirationBufferSeconds,
            accountUrl = "",
            thumbnailImageType = IgdbProvider.IgdbImageType.thumb_2x,
            posterImageType = IgdbProvider.IgdbImageType.screenshot_huge,
            screenshotImageType = IgdbProvider.IgdbImageType.screenshot_huge,
            defaultOrder = GameProvider.OrderPriorities.default,
            platforms = mapOf(platform.name to platformId),
            genres = mapOf(genreId.toString() to genre)
        )
        val provider = IgdbProvider(config, IgdbClient(config, storage))
    }
}
