package com.gitlab.ykrasik.gamedex.provider.giantbomb

import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.gitlab.ykrasik.gamedex.GamedexException
import com.gitlab.ykrasik.gamedex.Platform
import com.gitlab.ykrasik.gamedex.ProviderOrderPriorities
import com.gitlab.ykrasik.gamedex.test.*
import io.kotlintest.Spec
import io.kotlintest.TestCaseContext
import io.kotlintest.matchers.shouldBe
import io.kotlintest.matchers.shouldHave
import io.kotlintest.matchers.shouldThrow
import io.kotlintest.matchers.substring
import org.eclipse.jetty.http.HttpStatus

/**
 * User: ykrasik
 * Date: 19/04/2017
 * Time: 21:06
 */
class GiantBombClientIT : ScopedWordSpec() {
    val port = 9000
    val server = GiantBombMockServer(port)

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

            "throw GameDexException on invalid http response status" test {
                server.anySearchRequest() willFailWith HttpStatus.BAD_REQUEST_400

                val e = shouldThrow<GamedexException> {
                    client.search(name, platform, account)
                }
                e.message!! shouldHave substring(HttpStatus.BAD_REQUEST_400.toString())
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

            "throw GameDexException on invalid http response status" test {
                server.aFetchRequest(detailPath) willFailWith HttpStatus.BAD_REQUEST_400

                val e = shouldThrow<GamedexException> {
                    client.fetch(detailUrl, account)
                }
                e.message!! shouldHave substring(HttpStatus.BAD_REQUEST_400.toString())
            }
        }
    }

    inner class Scope {
        val baseUrl = "http://localhost:$port/"
        val detailPath = randomPath()
        val detailUrl = "$baseUrl$detailPath"
        val apiKey = randomString()
        val platform = randomEnum<Platform>()
        val platformId = rnd.nextInt(100)
        val name = randomName()

        private fun randomImage() = GiantBombClient.Image(thumbUrl = randomUrl(), superUrl = randomUrl())

        val searchResponse = GiantBombClient.SearchResponse(
            statusCode = GiantBombClient.Status.ok,
            results = listOf(GiantBombClient.SearchResult(
                apiDetailUrl = randomUrl(),
                name = randomName(),
                originalReleaseDate = randomLocalDate(),
                image = randomImage()
            ))
        )

        val detailsResponse = GiantBombClient.DetailsResponse(
            statusCode = GiantBombClient.Status.ok,
            results = listOf(GiantBombClient.DetailsResult(
                siteDetailUrl = randomUrl(),
                name = randomName(),
                deck = randomSentence(),
                originalReleaseDate = randomLocalDate(),
                image = randomImage(),
                images = listOf(randomImage(), randomImage()),
                genres = listOf(GiantBombClient.Genre(name = randomName()))
            ))
        )

        val account = GiantBombUserAccount(apiKey = apiKey)

        val client = GiantBombClient(GiantBombConfig(
            endpoint = baseUrl,
            noImageFileName = "",
            accountUrl = "",
            defaultOrder = ProviderOrderPriorities.default,
            platforms = mapOf(platform.name to platformId)
        ))
    }

    val searchFields = listOf("api_detail_url", "name", "original_release_date", "image")
    val fetchDetailsFields = searchFields - "api_detail_url" + listOf("site_detail_url", "deck", "genres", "images")

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