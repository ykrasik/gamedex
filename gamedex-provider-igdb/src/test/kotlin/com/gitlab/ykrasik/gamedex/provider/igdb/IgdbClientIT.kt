package com.gitlab.ykrasik.gamedex.provider.igdb

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
 * Date: 02/04/2017
 * Time: 21:02
 */
class IgdbClientIT : ScopedWordSpec() {
    val port = 9001
    val server = IgdbMockServer(port)

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
                        .withQueryParam("filter[release_dates.platform][eq]", platformId.toString())
                        .withQueryParam("limit", maxSearchResults.toString())
                        .withQueryParam("fields", searchFields.joinToString(","))
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
                server.aFetchRequest(id) willReturn detailsResult

                client.fetch(detailUrl, account) shouldBe detailsResult

                server.verify(
                    getRequestedFor(urlPathEqualTo("/$id"))
                        .withHeader("Accept", "application/json")
                        .withHeader("user-key", apiKey)
                        .withQueryParam("fields", fetchDetailsFields.joinToString(","))
                )
            }

            "throw GameDexException on invalid http response status" test {
                server.aFetchRequest(id) willFailWith HttpStatus.BAD_REQUEST_400

                val e = shouldThrow<GamedexException> {
                    client.fetch(detailUrl, account)
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
        val platform = randomEnum<Platform>()
        val platformId = rnd.nextInt(100)
        val name = randomName()

        val searchResult = IgdbClient.SearchResult(
            id = rnd.nextInt(),
            name = randomName(),
            aggregatedRating = randomScore().score,
            aggregatedRatingCount = randomScore().numReviews,
            rating = randomScore().score,
            ratingCount = randomScore().numReviews,
            releaseDates = listOf(randomReleaseDate()),
            cover = randomImage()
        )

        val detailsResult = IgdbClient.DetailsResult(
            url = randomString(),
            name = randomName(),
            summary = randomSentence(),
            releaseDates = listOf(randomReleaseDate()),
            aggregatedRating = randomScore().score,
            aggregatedRatingCount = randomScore().numReviews,
            rating = randomScore().score,
            ratingCount = randomScore().numReviews,
            cover = randomImage(),
            screenshots = listOf(randomImage(), randomImage()),
            genres = listOf(rnd.nextInt(100))
        )

        private fun randomReleaseDate() = IgdbClient.ReleaseDate(
            platform = platformId,
            category = 0,
            human = randomLocalDate().toString("YYYY-MMM-dd")
        )

        private fun randomImage() = IgdbClient.Image(cloudinaryId = randomString())

        val account = IgdbUserAccount(apiKey = apiKey)

        val client = IgdbClient(IgdbConfig(
            endpoint = baseUrl,
            baseImageUrl = baseImageUrl,
            accountUrl = "",
            maxSearchResults = maxSearchResults,
            thumbnailImageType = IgdbProvider.IgdbImageType.thumb_2x,
            posterImageType = IgdbProvider.IgdbImageType.screenshot_huge,
            screenshotImageType = IgdbProvider.IgdbImageType.screenshot_huge,
            defaultOrder = ProviderOrderPriorities.default,
            platforms = mapOf(platform.name to platformId),
            genres = emptyMap()
        ))
    }

    val searchFields = listOf(
        "name",
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
        "summary",
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

    private infix fun String.test(test: Scope.() -> Unit) = inScope(::Scope, test)
}