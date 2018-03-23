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
import com.gitlab.ykrasik.gamedex.util.toJsonStr
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.pipeline.PipelineContext
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.*
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.coroutines.experimental.delay
import java.io.Closeable
import java.util.concurrent.TimeUnit

/**
 * User: ykrasik
 * Date: 19/03/2017
 * Time: 16:48
 */
class IgdbMockServer(port: Int) : Closeable {
    private val wiremock = WireMockServer(port)

    fun start() = wiremock.start()
    fun reset() = wiremock.resetAll()
    override fun close() = wiremock.stop()

    fun verify(requestPatternBuilder: RequestPatternBuilder) = wiremock.verify(requestPatternBuilder)

    abstract inner class BaseRequest {
        infix fun willFailWith(status: Int) {
            wiremock.givenThat(get(anyUrl()).willReturn(aResponse().withStatus(status)))
        }
    }

    inner class anySearchRequest : BaseRequest() {
        infix fun willReturn(results: List<IgdbClient.SearchResult>) {
            wiremock.givenThat(get(urlPathEqualTo("/")).willReturn(aJsonResponse(results)))
        }
    }

    inner class aFetchRequest(private val gameId: Int) : BaseRequest() {
        infix fun willReturn(response: IgdbClient.DetailsResult) {
            wiremock.givenThat(get(urlPathEqualTo("/$gameId")).willReturn(aJsonResponse(listOf(response))))
        }
    }
}

class IgdbFakeServer(port: Int, private val apiKey: String) : Closeable {
    private val imagePath = "images"
    private val thumbnailPath = "t_thumb_2x"
    private val posterPath = "t_screenshot_huge"

    val providerId = "Igdb"
    val endpointUrl = "http://localhost:$port"
    fun detailsUrl(id: Int) = "$endpointUrl/$id"
    val baseImageUrl = "$endpointUrl/$imagePath"
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
                                delay(100)
                                call.respondText(randomSearchResults(name), ContentType.Application.Json)
                            }
                        }
                    }
                }
            }
            get("/{id}") {
                authorized {
                    delay(100)
                    call.respondText(randomDetailResponse(), ContentType.Application.Json)
                }
            }
            get("$imagePath/$thumbnailPath/{imageName}") {
                delay(400)
                call.respond(TestImages.randomImage())
            }
            get("$imagePath/$posterPath/{imageName}") {
                delay(600)
                call.respond(TestImages.randomImage())     // TODO: Use a different set of images
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

    private suspend fun delay(millis: Int) = delay(rnd.nextInt(millis).toLong(), TimeUnit.MILLISECONDS)

    private fun randomSearchResults(name: String): String = List(rnd.nextInt(10)) {
        IgdbClient.SearchResult(
            id = rnd.nextInt(),
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
        summary = randomSentence(maxWords = 50),
        releaseDates = randomReleaseDates(),
        aggregatedRating = randomScore().score,
        aggregatedRatingCount = randomScore().numReviews,
        rating = randomScore().score,
        ratingCount = randomScore().numReviews,
        cover = IgdbClient.Image(cloudinaryId = randomString()),
        screenshots = List(rnd.nextInt(10)) { randomImage() },
        genres = List(rnd.nextInt(4)) {
            listOf(2, 4, 5, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 24, 25, 26, 30, 31, 32, 33).randomElement()
        }
    )).toJsonStr()

    private fun randomReleaseDates() = List(rnd.nextInt(4)) { randomReleaseDate() }

    private fun randomReleaseDate(): IgdbClient.ReleaseDate {
        val category = rnd.nextInt(8)
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

    private fun randomImage() = IgdbClient.Image(cloudinaryId = randomString())

    fun start() = apply {
        ktor.start(wait = false)
    }

    override fun close() {
        ktor.stop(gracePeriod = 100, timeout = 100, timeUnit = TimeUnit.MILLISECONDS)
    }
}