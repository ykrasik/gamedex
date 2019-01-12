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

package com.gitlab.ykrasik.gamedex.provider.giantbomb

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder
import com.gitlab.ykrasik.gamedex.test.*
import com.gitlab.ykrasik.gamedex.util.filterNullValues
import com.gitlab.ykrasik.gamedex.util.freePort
import com.gitlab.ykrasik.gamedex.util.toJsonStr
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.util.pipeline.PipelineContext
import kotlinx.coroutines.delay
import java.io.Closeable
import java.util.concurrent.TimeUnit

/**
 * User: ykrasik
 * Date: 19/03/2017
 * Time: 13:14
 */
class GiantBombMockServer(port: Int = freePort) : Closeable {
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

    @Suppress("ClassName")
    inner class anySearchRequest : BaseRequest() {
        infix fun willReturn(response: GiantBombClient.SearchResponse) {
            wiremock.givenThat(get(urlPathEqualTo("/")).willReturn(aJsonResponse(response.toMap())))
        }
    }

    @Suppress("ClassName")
    inner class aFetchRequest(private val path: String) : BaseRequest() {
        infix fun willReturn(response: GiantBombClient.DetailsResponse) {
            wiremock.givenThat(get(urlPathEqualTo("/$path")).willReturn(aJsonResponse(response.toMap())))
        }
    }
}

class GiantBombFakeServer(port: Int = freePort, private val apiKey: String) : Closeable {
    private val apiDetailPath = "details"
    private val thumbnailPath = "images/thumbnail"
    private val superPath = "images/super"

    val providerId = "GiantBomb"
    val baseUrl = "http://localhost:$port"
    val apiDetailsUrl = "$baseUrl/$apiDetailPath"
    val thumbnailUrl = "$baseUrl/$thumbnailPath"
    val superUrl = "$baseUrl/$superPath"
    val screenshotUrl = superUrl

    private val ktor = embeddedServer(Netty, port) {
        routing {
            get("/") {
                authorized {
                    delay(50, 1000)
                    call.respondText(randomSearchResponse().toMap().toJsonStr(), ContentType.Application.Json)
                }
            }
            get("$thumbnailPath/{imageName}") {
                delay(200, 600)
                call.respond(com.gitlab.ykrasik.gamedex.test.randomImage())
            }
            get("$superPath/{imageName}") {
                delay(300, 800)
                call.respond(com.gitlab.ykrasik.gamedex.test.randomImage())     // TODO: Return a different set of images
            }
            get(apiDetailPath) {
                authorized {
                    delay(400, 1400)
                    call.respondText(randomDetailResponse().toMap().toJsonStr(), ContentType.Application.Json)
                }
            }
        }
    }

    private suspend fun PipelineContext<Unit, ApplicationCall>.authorized(body: suspend () -> Unit) {
        if (call.parameters["api_key"] == apiKey || System.getProperty("gameDex.provider.skipCredentialValidation") != null) {
            body()
        } else {
            call.respond(HttpStatusCode.Unauthorized, GiantBombClient.SearchResponse(GiantBombClient.Status.InvalidApiKey, emptyList()).toMap().toJsonStr())
        }
    }

    private suspend fun delay(minMillis: Int, maxMillis: Int) = delay(randomInt(min = minMillis, max = maxMillis).toLong())

    private fun randomSearchResponse() = GiantBombClient.SearchResponse(
        statusCode = GiantBombClient.Status.OK,
        results = randomList(10) {
            GiantBombClient.SearchResult(
                apiDetailUrl = apiDetailsUrl,
                name = randomName(),
                deck = randomParagraph(),
                originalReleaseDate = randomLocalDate(),
                image = randomImage()
            )
        }
    )

    private fun randomDetailResponse() = GiantBombClient.DetailsResponse(
        statusCode = GiantBombClient.Status.OK,
        results = listOf(GiantBombClient.DetailsResult(
            siteDetailUrl = randomUrl(),
            name = randomName(),
            deck = randomParagraph(),
            originalReleaseDate = randomLocalDate(),
            image = randomImage(),
            images = randomList(10) { randomImage() },
            genres = randomList(4) {
                GiantBombClient.Genre(name = randomGenre())
            }
        ))
    )

    private fun randomImage() = GiantBombClient.Image(
        thumbUrl = "$thumbnailUrl/${randomWord()}",
        superUrl = "$superUrl/${randomWord()}"
    )

    fun start() = apply {
        ktor.start()
    }

    override fun close() = ktor.stop(gracePeriod = 100, timeout = 100, timeUnit = TimeUnit.MILLISECONDS)
}

private fun GiantBombClient.SearchResponse.toMap() = mapOf(
    "error" to statusCode.asString(),
    "limit" to 100,
    "offset" to 0,
    "number_of_page_results" to 1,
    "number_of_total_results" to 1,
    "status_code" to statusCode.statusCode,
    "results" to results.map { it.toMap() },
    "version" to "1.0"
)

private fun GiantBombClient.SearchResult.toMap(): Map<String, Any> = mapOf(
    "api_detail_url" to apiDetailUrl,
    "name" to name,
    "deck" to deck,
    "original_release_date" to originalReleaseDate?.let { "$it 00:00:00" },
    "image" to image?.toMap()
).filterNullValues()

private fun GiantBombClient.DetailsResponse.toMap() = mapOf(
    "error" to statusCode.asString(),
    "limit" to 1,
    "offset" to 0,
    "number_of_page_results" to 1,
    "number_of_total_results" to 1,
    "status_code" to statusCode.statusCode,
    "results" to if (results.isNotEmpty()) results.first().toMap() else emptyList<Any>(),
    "version" to "1.0"
)

private fun GiantBombClient.DetailsResult.toMap(): Map<String, Any> = mapOf(
    "site_detail_url" to siteDetailUrl,
    "name" to name,
    "images" to images,
    "deck" to deck,
    "original_release_date" to originalReleaseDate?.let { "$it 00:00:00" },
    "image" to image?.toMap(),
    "genres" to genres?.map { it.toMap() }
).filterNullValues()

private fun GiantBombClient.Image.toMap() = mapOf(
    "icon_url" to randomUrl(),
    "medium_url" to randomUrl(),
    "screen_url" to randomUrl(),
    "small_url" to randomUrl(),
    "super_url" to superUrl,
    "thumb_url" to thumbUrl,
    "tiny_url" to randomUrl()
)

private fun GiantBombClient.Genre.toMap() = mapOf(
    "api_detail_url" to randomUrl(),
    "id" to randomInt(100),
    "name" to name,
    "site_detail_url" to randomUrl()
)

private fun GiantBombClient.Status.asString() = this.toString().toUpperCase()