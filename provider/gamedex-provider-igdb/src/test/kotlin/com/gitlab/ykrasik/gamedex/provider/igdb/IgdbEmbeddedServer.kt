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

package com.gitlab.ykrasik.gamedex.provider.igdb

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder
import com.gitlab.ykrasik.gamedex.test.*
import com.gitlab.ykrasik.gamedex.util.freePort
import com.gitlab.ykrasik.gamedex.util.toJsonStr
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.*
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.util.pipeline.PipelineContext
import kotlinx.coroutines.delay
import java.io.Closeable
import java.util.concurrent.TimeUnit

/**
 * User: ykrasik
 * Date: 19/03/2017
 * Time: 16:48
 */
class IgdbMockServer(port: Int = freePort) : Closeable {
    val baseUrl = "http://localhost:$port"
    private val wiremock = WireMockServer(port)

    fun start() = wiremock.start()
    fun reset() = wiremock.resetAll()
    override fun close() = wiremock.stop()

    fun verify(requestPatternBuilder: RequestPatternBuilder) = wiremock.verify(requestPatternBuilder)

    abstract inner class BaseRequest {
        infix fun willFailWith(status: HttpStatusCode) {
            wiremock.givenThat(get(anyUrl()).willReturn(aResponse().withStatus(status.value)))
        }
    }

    inner class anySearchRequest : BaseRequest() {
        infix fun willReturn(results: List<IgdbClient.SearchResult>) {
            wiremock.givenThat(get(urlPathEqualTo("/")).willReturn(aJsonResponse(results)))
        }
    }

    inner class aFetchRequest(private val gameId: Int) : BaseRequest() {
        infix fun willReturn(response: IgdbClient.DetailsResult) = willReturn(listOf(response))

        infix fun willReturn(response: List<IgdbClient.DetailsResult>) {
            wiremock.givenThat(get(urlPathEqualTo("/$gameId")).willReturn(aJsonResponse(response)))
        }
    }
}

class IgdbFakeServer(port: Int = freePort, private val apiKey: String) : Closeable {
    private val imagePath = "images"
    private val thumbnailPath = "t_thumb_2x"
    private val posterPath = "t_screenshot_huge"

    val providerId = "Igdb"
    val baseUrl = "http://localhost:$port"
    fun detailsUrl(id: Int) = "$baseUrl/$id"
    val baseImageUrl = "$baseUrl/$imagePath"
    val thumbnailUrl = "$baseImageUrl/$thumbnailPath"
    val posterUrl = "$baseImageUrl/$posterPath"
    val screenshotUrl = posterUrl

    private val ktor = embeddedServer(Netty, port) {
        routing {
            route("/") {
                method(HttpMethod.Get) {
                    param("search") {
                        handle {
                            authorized {
                                val name = call.parameters["search"]!!
                                delay(50, 1000)
                                call.respondText(randomSearchResults(name), ContentType.Application.Json)
                            }
                        }
                    }
                }
            }
            get("/{id}") {
                authorized {
                    delay(200, 800)
                    call.respondText(randomDetailResponse(), ContentType.Application.Json)
                }
            }
            get("$imagePath/$thumbnailPath/{imageName}") {
                delay(100, 600)
                call.respond(com.gitlab.ykrasik.gamedex.test.randomImage())
            }
            get("$imagePath/$posterPath/{imageName}") {
                delay(200, 800)
                call.respond(com.gitlab.ykrasik.gamedex.test.randomImage())     // TODO: Use a different set of images
            }
        }
    }

    private suspend fun PipelineContext<Unit, ApplicationCall>.authorized(body: suspend () -> Unit) {
        if (call.request.headers["user-key"] == apiKey) {
            body()
        } else {
            call.respond(HttpStatusCode.Unauthorized)
        }
    }

    private suspend fun delay(minMillis: Int, maxMillis: Int) = delay(randomInt(min = minMillis, max = maxMillis).toLong())

    private fun randomSearchResults(name: String): String = randomList(10) {
        IgdbClient.SearchResult(
            id = randomInt(),
            name = "$name ${randomName()}",
            aggregatedRating = randomScore().score,
            aggregatedRatingCount = randomScore().numReviews,
            rating = randomScore().score,
            ratingCount = randomScore().numReviews,
            releaseDates = randomReleaseDates(),
            cover = randomImage()
        )
    }.toJsonStr()

    private fun randomPlatform() = listOf(6, 9, 12, 48, 49).randomElement()

    private fun randomDetailResponse(): String = listOf(IgdbClient.DetailsResult(
        url = randomUrl(),
        name = randomName(),
        summary = randomParagraph(),
        releaseDates = randomReleaseDates(),
        aggregatedRating = randomScore().score,
        aggregatedRatingCount = randomScore().numReviews,
        rating = randomScore().score,
        ratingCount = randomScore().numReviews,
        cover = IgdbClient.Image(cloudinaryId = randomWord()),
        screenshots = randomList(10) { randomImage() },
        genres = randomList(4) {
            listOf(2, 4, 5, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 24, 25, 26, 30, 31, 32, 33).randomElement()
        }
    )).toJsonStr()

    private fun randomReleaseDates() = randomList(3) { randomReleaseDate() }

    private fun randomReleaseDate(): IgdbClient.ReleaseDate {
        val category = randomInt(7)
        val date = randomLocalDate()
        val human = when (category) {
            0 -> date.toString("YYYY-MMM-dd")
            1 -> date.toString("YYYY-MMM")
            2 -> date.toString("YYYY")
            3 -> date.toString("YYYY-'Q1'")
            4 -> date.toString("YYYY-'Q2'")
            5 -> date.toString("YYYY-'Q3'")
            6 -> date.toString("YYYY-'Q4'")
            7 -> "TBD"
            else -> error("Unsupported format: $category")
        }
        return IgdbClient.ReleaseDate(
            platform = randomPlatform(),
            category = category,
            human = human
        )
    }

    private fun randomImage() = IgdbClient.Image(cloudinaryId = randomWord())

    fun start() = apply {
        ktor.start(wait = false)
    }

    override fun close() {
        ktor.stop(gracePeriod = 100, timeout = 100, timeUnit = TimeUnit.MILLISECONDS)
    }
}