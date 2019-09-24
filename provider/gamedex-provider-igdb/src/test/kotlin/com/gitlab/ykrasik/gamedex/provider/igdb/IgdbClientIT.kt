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
import com.gitlab.ykrasik.gamedex.Platform
import com.gitlab.ykrasik.gamedex.provider.ProviderOrderPriorities
import com.gitlab.ykrasik.gamedex.test.*
import io.kotlintest.Spec
import io.kotlintest.TestCaseContext
import io.kotlintest.matchers.shouldBe
import io.kotlintest.matchers.shouldHave
import io.kotlintest.matchers.shouldThrow
import io.kotlintest.matchers.substring
import io.ktor.client.features.ClientRequestException
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
                server.anyRequest() willReturnSearch listOf(searchResult)

                val name = "testName"
                val search = client.search(name, platform, account, offset, limit)
                search shouldBe listOf(searchResult)

                server.verify(
                    postRequestedFor(urlPathEqualTo("/"))
                        .withHeader("Accept", "application/json")
                        .withHeader("user-key", apiKey)
                        .withBody(
                            """
                                search "$name";
                                fields ${searchFields.joinToString(",")};
                                where name ~ *"$name"* & release_dates.platform = $platformId;
                                offset $offset;
                                limit $limit;
                            """.trimIndent()
                        )
                )
            }

            "split search name by whitespace & remove non-word characters" test {
                server.anyRequest() willReturnSearch listOf(searchResult)

                client.search("name with\tspace & other-characters: yes\nno,   maybe", platform, account, offset, limit)

                server.verify(
                    postRequestedFor(urlPathEqualTo("/"))
                        .withBodyContaining(
                            """
                                where name ~ *"name"* & name ~ *"with"* & name ~ *"space"* & name ~ *"other"* & name ~ *"characters"* & name ~ *"yes"* & name ~ *"no"* & name ~ *"maybe"*
                            """.trimIndent()
                        )
                )
            }

            "throw IllegalStateException on invalid http response status" test {
                server.anyRequest() willFailWith HttpStatusCode.BadRequest

                val e = shouldThrow<ClientRequestException> {
                    client.search(name, platform, account, offset, limit)
                }
                e.message!! shouldHave substring(HttpStatusCode.BadRequest.value.toString())
            }
        }

        "fetch" should {
            "fetch by providerGameId" test {
                server.anyRequest() willReturnFetch listOf(detailsResult)

                client.fetch(id, account) shouldBe detailsResult

                server.verify(
                    postRequestedFor(urlPathEqualTo("/"))
                        .withHeader("Accept", "application/json")
                        .withHeader("user-key", apiKey)
                        .withBody(
                            """
                                fields ${fetchDetailsFields.joinToString(",")};
                                where id = $id;
                            """.trimIndent()
                        )
                )
            }

            "return null on empty response" test {
                server.anyRequest() willReturnFetch emptyList()

                client.fetch(id, account) shouldBe null
            }

            "throw ClientRequestException on invalid http response status" test {
                server.anyRequest() willFailWith HttpStatusCode.BadRequest

                val e = shouldThrow<ClientRequestException> {
                    client.fetch(id, account)
                }
                e.response.status shouldBe HttpStatusCode.BadRequest
            }
        }
    }

    inner class Scope {
        val baseImageUrl = "${server.baseUrl}/images"
        val id = randomInt().toString()
        val apiKey = randomWord()
        val platform = randomEnum<Platform>()
        val platformId = randomInt(100)
        val name = randomName()
        val offset = randomInt(100)
        val limit = randomInt(max = 100, min = 1)

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

        private fun randomImage() = IgdbClient.Image(imageId = randomString())

        val account = IgdbUserAccount(apiKey = apiKey)

        val client = IgdbClient(
            IgdbConfig(
                baseUrl = server.baseUrl,
                baseImageUrl = baseImageUrl,
                accountUrl = "",
                thumbnailImageType = IgdbProvider.IgdbImageType.thumb_2x,
                posterImageType = IgdbProvider.IgdbImageType.screenshot_huge,
                screenshotImageType = IgdbProvider.IgdbImageType.screenshot_huge,
                defaultOrder = ProviderOrderPriorities.default,
                platforms = mapOf(platform.name to platformId),
                genres = emptyMap()
            )
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
        "cover.image_id"
    )

    val fetchDetailsFields = searchFields + listOf(
        "url",
        "screenshots.image_id",
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