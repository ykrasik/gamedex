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

import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.gitlab.ykrasik.gamedex.Platform
import com.gitlab.ykrasik.gamedex.provider.ProviderOrderPriorities
import com.gitlab.ykrasik.gamedex.test.*
import com.gitlab.ykrasik.gamedex.util.urlEncoded
import io.kotlintest.Spec
import io.kotlintest.TestCaseContext
import io.kotlintest.matchers.shouldBe
import io.kotlintest.matchers.shouldHave
import io.kotlintest.matchers.shouldThrow
import io.kotlintest.matchers.substring
import io.ktor.http.HttpStatusCode

/**
 * User: ykrasik
 * Date: 02/04/2017
 * Time: 21:02
 */
class IgdbClientIT : ScopedWordSpec<IgdbClientIT.Scope>() {
    val server = IgdbMockServer()

    override fun scope() = Scope()

    init {
        "search" should {
            "search by name & platform" test {
                server.anySearchRequest() willReturn listOf(searchResult)

                client.search(name, platform, account) shouldBe listOf(searchResult)

                server.verify(
                    getRequestedFor(urlPathEqualTo("/"))
                        .withHeader("Accept", "application/json")
                        .withHeader("user-key", apiKey)
                        .withQueryParam("search", name)
                        .withQueryParam("filter[release_dates.platform][eq]".urlEncoded(), platformId.toString())
                        .withQueryParam("limit", maxSearchResults.toString())
                        .withQueryParam("fields", searchFields.joinToString(","))
                )
            }

            "throw IllegalStateException on invalid http response status" test {
                server.anySearchRequest() willFailWith HttpStatusCode.BadRequest

                val e = shouldThrow<IllegalStateException> {
                    client.search(name, platform, account)
                }
                e.message!! shouldHave substring(HttpStatusCode.BadRequest.value.toString())
            }
        }

        "fetch" should {
            "fetch by url" test {
                server.aFetchRequest(id) willReturn detailsResult

                client.fetch(detailUrl, account) shouldBe detailsResult

                server.verify(
                    getRequestedFor(urlPathEqualTo("/$id"))
                        .withHeader("Accept", "application/json")
                        .withHeader("user-key", apiKey)
                        .withQueryParam("fields", fetchDetailsFields.joinToString(","))
                )
            }

            "throw IllegalStateException on empty response" test {
                server.aFetchRequest(id) willReturn emptyList()

                val e = shouldThrow<IllegalStateException> {
                    client.fetch(detailUrl, account)
                }
                e.message!! shouldHave substring("Not Found!")
            }

            "throw IllegalStateException on invalid http response status" test {
                server.aFetchRequest(id) willFailWith HttpStatusCode.BadRequest

                val e = shouldThrow<IllegalStateException> {
                    client.fetch(detailUrl, account)
                }
                e.message!! shouldHave substring(HttpStatusCode.BadRequest.value.toString())
            }
        }
    }

    inner class Scope {
        val baseImageUrl = "${server.baseUrl}/images"
        val id = randomInt()
        val detailUrl = "${server.baseUrl}/$id"
        val apiKey = randomWord()
        val maxSearchResults = randomInt()
        val platform = randomEnum<Platform>()
        val platformId = randomInt(100)
        val name = randomName()

        val searchResult = IgdbClient.SearchResult(
            id = randomInt(),
            name = randomName(),
            summary = randomParagraph(),
            aggregatedRating = randomScore().score,
            aggregatedRatingCount = randomScore().numReviews,
            rating = randomScore().score,
            ratingCount = randomScore().numReviews,
            releaseDates = listOf(randomReleaseDate()),
            cover = randomImage()
        )

        val detailsResult = IgdbClient.DetailsResult(
            url = randomWord(),
            name = randomName(),
            summary = randomParagraph(),
            releaseDates = listOf(randomReleaseDate()),
            aggregatedRating = randomScore().score,
            aggregatedRatingCount = randomScore().numReviews,
            rating = randomScore().score,
            ratingCount = randomScore().numReviews,
            cover = randomImage(),
            screenshots = listOf(randomImage(), randomImage()),
            genres = listOf(randomInt(100))
        )

        private fun randomReleaseDate() = IgdbClient.ReleaseDate(
            platform = platformId,
            category = 0,
            human = randomLocalDate().toString("YYYY-MMM-dd")
        )

        private fun randomImage() = IgdbClient.Image(cloudinaryId = randomWord())

        val account = IgdbUserAccount(apiKey = apiKey)

        val client = IgdbClient(
            IgdbConfig(
                baseUrl = server.baseUrl,
                baseImageUrl = baseImageUrl,
                accountUrl = "",
                maxSearchResults = maxSearchResults,
                thumbnailImageType = IgdbProvider.IgdbImageType.thumb_2x,
                posterImageType = IgdbProvider.IgdbImageType.screenshot_huge,
                screenshotImageType = IgdbProvider.IgdbImageType.screenshot_huge,
                defaultOrder = ProviderOrderPriorities.default,
                platforms = mapOf(platform.name to platformId),
                genres = emptyMap()
            ),
            testHttpClient
        )
    }

    val searchFields = listOf(
        "name",
        "summary",
        "aggregated_rating",
        "aggregated_rating_count",
        "rating",
        "rating_count",
        "release_dates.category",
        "release_dates.human",
        "release_dates.platform",
        "cover.cloudinary_id"
    )

    val fetchDetailsFields = searchFields + listOf(
        "url",
        "screenshots.cloudinary_id",
        "genres"
    )

    override fun interceptSpec(context: Spec, spec: () -> Unit) {
        server.start()
        spec()
        server.close()
    }

    override fun interceptTestCase(context: TestCaseContext, test: () -> Unit) {
        server.reset()
        test()
    }
}