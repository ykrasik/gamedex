package com.gitlab.ykrasik.gamedex.provider.igdb

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
 * Date: 02/04/2017
 * Time: 21:02
 */
class IgdbClientIT : ScopedWordSpec() {
    val port = 9001
    val server = IgdbMockServer(port)

    init {
        "IgdbClient.search" should {
            "search by name & platform".inScope(Scope()) {
                server.anySearchRequest() willReturn listOf(searchResult)

                client.search(name, platform) shouldBe listOf(searchResult)

                server.verify(
                    getRequestedFor(urlPathEqualTo("/"))
                        .withHeader("Accept", "application/json")
                        .withHeader("X-Mashape-Key", apiKey)
                        .withQueryParam("search", name)
                        .withQueryParam("filter[release_dates.platform][eq]", platformId.toString())
                        .withQueryParam("limit", maxSearchResults.toString())
                        .withQueryParam("fields", searchFields.joinToString(","))
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

        "IgdbClient.fetch" should {
            "fetch by url".inScope(Scope()) {
                server.aFetchRequest(id) willReturn detailsResult

                client.fetch(detailUrl) shouldBe detailsResult

                server.verify(
                    getRequestedFor(urlPathEqualTo("/$id"))
                        .withHeader("Accept", "application/json")
                        .withHeader("X-Mashape-Key", apiKey)
                        .withQueryParam("fields", fetchDetailsFields.joinToString(","))
                )
            }

            "throw GameDexException on invalid http response status".inScope(Scope()) {
                server.aFetchRequest(id) willFailWith HttpStatus.BAD_REQUEST_400

                val e = shouldThrow<GameDexException> {
                    client.fetch(detailUrl)
                }
                e.message!! shouldHave substring(HttpStatus.BAD_REQUEST_400.toString())
            }
        }
    }

    inner class Scope {
        val baseUrl = "http://localhost:$port"
        val baseImageUrl = "$baseUrl/images"
        val id = rnd.nextInt()
        val detailUrl = "$baseUrl/$id"
        val apiKey = randomString()
        val maxSearchResults = rnd.nextInt()
        val platform = randomEnum<GamePlatform>()
        val platformId = rnd.nextInt(100)
        val name = randomName()

        val searchResult = IgdbClient.SearchResult(
            id = rnd.nextInt(),
            name = randomName(),
            aggregatedRating = randomScore(),
            releaseDates = listOf(IgdbClient.ReleaseDate(
                platform = platformId,
                category = 0,
                human = randomLocalDate().toString("YYYY-MMM-dd")
            )),
            cover = IgdbClient.Image(cloudinaryId = randomString())
        )

        val detailsResult = IgdbClient.DetailsResult(
            url = randomString(),
            name = randomName(),
            summary = randomSentence(),
            releaseDates = listOf(IgdbClient.ReleaseDate(
                platform = platformId,
                category = 0,
                human = randomLocalDate().toString("YYYY-MMM-dd")
            )),
            aggregatedRating = randomScore(),
            rating = randomScore(),
            cover = IgdbClient.Image(cloudinaryId = randomString()),
            screenshots = emptyList<IgdbClient.Image>(), // TODO: Support screenshots
            genres = listOf(rnd.nextInt(100))
        )

        val client = IgdbClient(IgdbConfig(
            endpoint = baseUrl,
            baseImageUrl = baseImageUrl,
            apiKey = apiKey,
            maxSearchResults = maxSearchResults,
            platforms = mapOf(platform to platformId),
            genres = emptyMap()
        ))
    }

    val searchFields = listOf(
        "name",
        "aggregated_rating",
        "release_dates.category",
        "release_dates.human",
        "release_dates.platform",
        "cover.cloudinary_id"
    )

    val fetchDetailsFields = searchFields + listOf(
        "url",
        "summary",
        "rating",
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