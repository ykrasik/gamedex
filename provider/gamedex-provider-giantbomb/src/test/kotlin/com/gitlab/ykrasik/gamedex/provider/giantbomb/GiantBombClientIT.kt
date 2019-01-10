/****************************************************************************
 * Copyright (C) 2016-2018 Yevgeny Krasik                                   *
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
import com.gitlab.ykrasik.gamedex.Platform
import com.gitlab.ykrasik.gamedex.provider.ProviderOrderPriorities
import com.gitlab.ykrasik.gamedex.test.*
import io.kotlintest.Spec
import io.kotlintest.TestCaseContext
import io.kotlintest.matchers.shouldBe
import io.kotlintest.matchers.shouldHave
import io.kotlintest.matchers.shouldThrow
import io.kotlintest.matchers.substring
import io.ktor.http.HttpStatusCode

/**
 * User: ykrasik
 * Date: 19/04/2017
 * Time: 21:06
 */
class GiantBombClientIT : ScopedWordSpec() {
    val server = GiantBombMockServer()

    init {
        "search" should {
            "search by name & platform" test {
                server.anySearchRequest() willReturn searchResponse

                client.search(name, platform, account) shouldBe searchResponse

                server.verify(
                    getRequestedFor(urlPathEqualTo("/"))
                        .withQueryParam("api_key", apiKey)
                        .withQueryParam("format", "json")
                        .withQueryParam("filter", "name:$name,platforms:$platformId")
                        .withQueryParam("field_list", searchFields.joinToString(","))
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
                server.aFetchRequest(detailPath) willReturn detailsResponse

                client.fetch(detailUrl, account) shouldBe detailsResponse

                server.verify(
                    getRequestedFor(urlPathEqualTo("/$detailPath"))
                        .withQueryParam("api_key", apiKey)
                        .withQueryParam("field_list", fetchDetailsFields.joinToString(","))
                )
            }

            "throw IllegalStateException on invalid http response status" test {
                server.aFetchRequest(detailPath) willFailWith HttpStatusCode.BadRequest

                val e = shouldThrow<IllegalStateException> {
                    client.fetch(detailUrl, account)
                }
                e.message!! shouldHave substring(HttpStatusCode.BadRequest.value.toString())
            }
        }
    }

    inner class Scope {
        val detailPath = randomPath()
        val detailUrl = "${server.baseUrl}/$detailPath"
        val apiKey = randomWord()
        val platform = randomEnum<Platform>()
        val platformId = randomInt(100)
        val name = randomName()

        private fun randomImage() = GiantBombClient.Image(thumbUrl = randomUrl(), superUrl = randomUrl())

        val searchResponse = GiantBombClient.SearchResponse(
            statusCode = GiantBombClient.Status.OK,
            results = listOf(
                GiantBombClient.SearchResult(
                    apiDetailUrl = randomUrl(),
                    name = randomName(),
                    deck = randomParagraph(),
                    originalReleaseDate = randomLocalDate(),
                    image = randomImage()
                )
            )
        )

        val detailsResponse = GiantBombClient.DetailsResponse(
            statusCode = GiantBombClient.Status.OK,
            results = listOf(
                GiantBombClient.DetailsResult(
                    siteDetailUrl = randomUrl(),
                    name = randomName(),
                    deck = randomParagraph(),
                    originalReleaseDate = randomLocalDate(),
                    image = randomImage(),
                    images = listOf(randomImage(), randomImage()),
                    genres = listOf(GiantBombClient.Genre(name = randomName()))
                )
            )
        )

        val account = GiantBombUserAccount(apiKey = apiKey)

        val client = GiantBombClient(
            GiantBombConfig(
                baseUrl = server.baseUrl,
                noImageFileName = "",
                accountUrl = "",
                defaultOrder = ProviderOrderPriorities.default,
                platforms = mapOf(platform.name to platformId)
            )
        )
    }

    val searchFields = listOf("api_detail_url", "name", "deck", "original_release_date", "image")
    val fetchDetailsFields = searchFields - "api_detail_url" + listOf("site_detail_url", "genres", "images")

    override fun interceptSpec(context: Spec, spec: () -> Unit) {
        server.start()
        spec()
        server.close()
    }

    override fun interceptTestCase(context: TestCaseContext, test: () -> Unit) {
        server.reset()
        test()
    }

    private infix fun String.test(test: Scope.() -> Unit) = inScope(::Scope, test)
}