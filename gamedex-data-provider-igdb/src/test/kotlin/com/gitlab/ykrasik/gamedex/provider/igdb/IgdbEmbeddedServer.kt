package com.gitlab.ykrasik.gamedex.provider.igdb

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.gitlab.ykrasik.gamedex.common.testkit.*
import com.gitlab.ykrasik.gamedex.common.util.toJsonStr
import com.gitlab.ykrasik.gamedex.provider.aJsonResponse
import com.gitlab.ykrasik.gamedex.provider.withHeader
import com.gitlab.ykrasik.gamedex.provider.withQueryParam
import kotlinx.coroutines.experimental.delay
import org.jetbrains.ktor.application.call
import org.jetbrains.ktor.http.ContentType
import org.jetbrains.ktor.http.HttpMethod
import org.jetbrains.ktor.netty.embeddedNettyServer
import org.jetbrains.ktor.response.respondText
import org.jetbrains.ktor.routing.*
import java.io.Closeable
import java.util.concurrent.TimeUnit

/**
 * User: ykrasik
 * Date: 19/03/2017
 * Time: 16:48
 */
class IgdbEmbeddedServer(port: Int) : Closeable {
    private val apiDetailPath = 1
    private val image = "image"

    private val searchFields = listOf(
        "name",
        "aggregated_rating",
        "release_dates.category",
        "release_dates.human",
        "release_dates.platform",
        "cover.cloudinary_id"
    ).joinToString(",")

    private val fetchDetailsFields = listOf(
        "url",
        "summary",
        "rating",
        "cover.cloudinary_id",
        "screenshots.cloudinary_id",
        "genres"
    ).joinToString(",")

    private val wiremock = WireMockServer(port)
    private val ktor = embeddedNettyServer(port) {
        routing {
            route("/") {
                method(HttpMethod.Get) {
                    param("search") {
                        handle {
                            val name = call.parameters["search"]!!
                            call.respondText(randomSearchResults(name), ContentType.Application.Json)
                        }
                    }
                }
            }
            get("images/t_thumb_2x/$image.png") {
                delay(rnd.nextInt(700).toLong(), TimeUnit.MILLISECONDS)
                call.respond(TestImages.randomImageBytes())
            }
            get(apiDetailPath.toString()) {
                call.respondText(randomDetailResponse(), ContentType.Application.Json)
            }
        }
    }

    private fun randomSearchResults(name: String): String = List(rnd.nextInt(20)) { IgdbSearchResult(
        id = apiDetailPath,
        name = "$name ${randomName()}",
        aggregatedRating = randomScore(),
        releaseDates = List(rnd.nextInt(4)) { randomReleaseDate() },
        cover = IgdbImage(
            cloudinaryId = image
        )
    ) }.toJsonStr()

    private fun randomReleaseDate(): IgdbReleaseDate {
        val category = rnd.nextInt(8)
        val date = randomLocalDate()
        val human = when(category) {
            0 -> date.toString("YYYY-MMM-dd")
            1 -> date.toString("YYYY-MMM")
            2 -> date.toString("YYYY")
            3 -> date.toString("YYYY-'Q1'")
            4 -> date.toString("YYYY-'Q2'")
            5 -> date.toString("YYYY-'Q3'")
            6 -> date.toString("YYYY-'Q4'")
            7 -> "TBD"
            else -> TODO()
        }
        return IgdbReleaseDate(
            platform = randomPlatform(),
            category = category,
            human = human
        )
    }

    private fun randomPlatform() = listOf(6, 9, 12, 48, 49).randomElement()

    private fun randomDetailResponse(): String = listOf(IgdbDetailsResult(
        url = randomUrl(),
        summary = randomSentence(maxWords = 50),
        rating = randomScore(),
        cover = IgdbImage(cloudinaryId = image),
        screenshots = List(rnd.nextInt(10)) { IgdbImage(cloudinaryId = image) },
        genres = List(rnd.nextInt(4)) {
            listOf(2, 4, 5, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 24, 25, 26, 30, 31, 32, 33).randomElement()
        }
    )).toJsonStr()

    inner class searchRequest(private val apiKey: String,
                              private val name: String,
                              private val platform: Int,
                              private val maxSearchResults: Int) {
        infix fun willReturn(results: List<IgdbSearchResult>) {
            wiremock.givenThat(aSearchRequest.willReturn(aJsonResponse(results)))
        }

        infix fun willFailWith(status: Int) {
            wiremock.givenThat(aSearchRequest.willReturn(aResponse().withStatus(status)))
        }

        private val aSearchRequest get() = get(urlPathEqualTo("/"))
            .withHeader("Accept", "application/json")
            .withHeader("X-Mashape-Key", apiKey)
            .withQueryParam("search", name)
            .withQueryParam("filter[release_dates.platform][eq]", platform.toString())
            .withQueryParam("limit", maxSearchResults.toString())
            .withQueryParam("fields", searchFields)
    }

    inner class fetchRequest(private val apiKey: String, private val gameId: Int) {
        infix fun willReturn(response: IgdbDetailsResult) {
            wiremock.givenThat(aFetchRequest.willReturn(aJsonResponse(listOf(response))))
        }

        infix fun willFailWith(status: Int) {
            wiremock.givenThat(aFetchRequest.willReturn(aResponse().withStatus(status)))
        }

        private val aFetchRequest get() = get(urlPathEqualTo("/$gameId"))
            .withHeader("Accept", "application/json")
            .withHeader("X-Mashape-Key", apiKey)
            .withQueryParam("fields", fetchDetailsFields)
    }

    fun start(isFakeProd: Boolean = false): IgdbEmbeddedServer = apply {
        if (isFakeProd) {
            ktor.start(wait = false)
        } else {
            wiremock.start()
        }
    }

    fun reset() {
        wiremock.resetAll()
    }

    override fun close() {
        ktor.stop(gracePeriod = 100, timeout = 100, timeUnit = TimeUnit.MILLISECONDS)
        wiremock.stop()
    }
}