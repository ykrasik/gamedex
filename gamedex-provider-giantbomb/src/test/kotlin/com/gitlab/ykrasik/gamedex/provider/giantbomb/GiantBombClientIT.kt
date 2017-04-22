package com.gitlab.ykrasik.gamedex.provider.giantbomb

import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.gitlab.ykrasik.gamedex.GameDexException
import com.gitlab.ykrasik.gamedex.GamePlatform
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
        "GiantBombClient.search" should {
            "search by name & platform".inScope(Scope()) {
                server.anySearchRequest() willReturn searchResponse

                client.search(name, platform) shouldBe searchResponse

                server.verify(
                    getRequestedFor(urlPathEqualTo("/"))
                        .withQueryParam("api_key", apiKey)
                        .withQueryParam("format", "json")
                        .withQueryParam("filter", "name:$name,platforms:$platformId")
                        .withQueryParam("field_list", searchFields.joinToString(","))
                )
            }

            "throw GameDexException on invalid http response status".inScope(Scope()) {
                server.anySearchRequest() willFailWith HttpStatus.BAD_REQUEST_400

                val e = shouldThrow<GameDexException> {
                    client.search(name, platform)
                }
                e.message!! shouldHave substring(HttpStatus.BAD_REQUEST_400.toString())
            }
        }

        "GiantBombClient.fetch" should {
            "fetch by url".inScope(Scope()) {
                server.aFetchRequest(detailPath) willReturn detailsResponse

                client.fetch(detailUrl) shouldBe detailsResponse

                server.verify(
                    getRequestedFor(urlPathEqualTo("/$detailPath"))
                        .withQueryParam("api_key", apiKey)
                        .withQueryParam("field_list", fetchDetailsFields.joinToString(","))
                )
            }

            "throw GameDexException on invalid http response status".inScope(Scope()) {
                server.aFetchRequest(detailPath) willFailWith HttpStatus.BAD_REQUEST_400

                val e = shouldThrow<GameDexException> {
                    client.fetch(detailUrl)
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
        val platform = randomEnum<GamePlatform>()
        val platformId = rnd.nextInt(100)
        val name = randomName()

        val searchResponse = GiantBomb.SearchResponse(
            statusCode = GiantBomb.Status.ok,
            results = listOf(GiantBomb.SearchResult(
                apiDetailUrl = randomUrl(),
                name = randomName(),
                originalReleaseDate = randomLocalDate(),
                image = GiantBomb.SearchImage(thumbUrl = randomUrl())
            ))
        )

        val detailsResponse = GiantBomb.DetailsResponse(
            statusCode = GiantBomb.Status.ok,
            results = listOf(GiantBomb.DetailsResult(
                siteDetailUrl = randomUrl(),
                name = randomName(),
                deck = randomSentence(),
                originalReleaseDate = randomLocalDate(),
                image = GiantBomb.DetailsImage(
                    thumbUrl = randomUrl(),
                    superUrl = randomUrl()
                ),
                genres = listOf(GiantBomb.Genre(name = randomName()))
            ))
        )

        val client = GiantBombClient(GiantBombConfig(
            endpoint = baseUrl,
            apiKey = apiKey,
            platforms = mapOf(platform to platformId)
        ))
    }

    val searchFields = listOf("api_detail_url", "name", "original_release_date", "image")
    val fetchDetailsFields = searchFields - "api_detail_url" + listOf("site_detail_url", "deck", "genres")

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