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

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder
import com.gitlab.ykrasik.gamedex.test.*
import com.gitlab.ykrasik.gamedex.util.toJsonStr
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.pipeline.PipelineContext
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.coroutines.experimental.delay
import java.io.Closeable
import java.util.concurrent.TimeUnit

/**
 * User: ykrasik
 * Date: 19/03/2017
 * Time: 13:14
 */
class GiantBombMockServer(port: Int) : Closeable {
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
        infix fun willReturn(response: GiantBombClient.SearchResponse) {
            wiremock.givenThat(get(urlPathEqualTo("/")).willReturn(aJsonResponse(response.toMap())))
        }
    }

    inner class aFetchRequest(private val path: String) : BaseRequest() {
        infix fun willReturn(response: GiantBombClient.DetailsResponse) {
            wiremock.givenThat(get(urlPathEqualTo("/$path")).willReturn(aJsonResponse(response.toMap())))
        }
    }
}

class GiantBombFakeServer(port: Int, private val apiKey: String) : Closeable {
    private val apiDetailPath = "details"
    private val thumbnailPath = "images/thumbnail"
    private val superPath = "images/super"

    val providerId = "GiantBomb"
    val endpointUrl = "http://localhost:$port"
    val apiDetailsUrl = "$endpointUrl/$apiDetailPath"
    val thumbnailUrl = "$endpointUrl/$thumbnailPath"
    val superUrl = "$endpointUrl/$superPath"
    val screenshotUrl = superUrl

    private val ktor = embeddedServer(Netty, port) {
        routing {
            get("/") {
                authorized {
                    delay(100)
                    call.respondText(randomSearchResponse().toMap().toJsonStr(), ContentType.Application.Json)
                }
            }
            get("$thumbnailPath/{imageName}") {
                delay(400)
                call.respond(TestImages.randomImage())
            }
            get("$superPath/{imageName}") {
                delay(600)
                call.respond(TestImages.randomImage())     // TODO: Return a different set of images
            }
            get(apiDetailPath) {
                authorized {
                    delay(100)
                    call.respondText(randomDetailResponse().toMap().toJsonStr(), ContentType.Application.Json)
                }
            }
        }
    }

    private suspend fun PipelineContext<Unit, ApplicationCall>.authorized(body: suspend () -> Unit) {
        if (call.parameters["api_key"] == apiKey) {
            body()
        } else {
            call.respond(HttpStatusCode.Unauthorized, GiantBombClient.SearchResponse(GiantBombClient.Status.invalidApiKey, emptyList()).toMap().toJsonStr())
        }
    }

    private suspend fun delay(millis: Int) = delay(rnd.nextInt(millis).toLong(), TimeUnit.MILLISECONDS)

    private fun randomSearchResponse() = GiantBombClient.SearchResponse(
        statusCode = GiantBombClient.Status.ok,
        results = List(rnd.nextInt(10)) {
            GiantBombClient.SearchResult(
                apiDetailUrl = apiDetailsUrl,
                name = randomName(),
                originalReleaseDate = randomLocalDate(),
                image = randomImage()
            )
        }
    )

    private fun randomDetailResponse() = GiantBombClient.DetailsResponse(
        statusCode = GiantBombClient.Status.ok,
        results = listOf(GiantBombClient.DetailsResult(
            siteDetailUrl = randomUrl(),
            name = randomName(),
            deck = randomSentence(maxWords = 15),
            originalReleaseDate = randomLocalDate(),
            image = randomImage(),
            images = List(rnd.nextInt(10)) { randomImage() },
            genres = List(rnd.nextInt(4)) {
                GiantBombClient.Genre(name = randomString())
            }
        ))
    )

    private fun randomImage() = GiantBombClient.Image(
        thumbUrl = "$thumbnailUrl/${randomString()}",
        superUrl = "$superUrl/${randomString()}"
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

private fun GiantBombClient.SearchResult.toMap(): Map<String, Any> {
    val map = mutableMapOf<String, Any>(
        "api_detail_url" to apiDetailUrl,
        "name" to name
    )
    if (originalReleaseDate != null) {
        map += ("original_release_date" to "$originalReleaseDate 00:00:00")
    }
    if (image != null) {
        map += ("image" to image!!.toMap())
    }
    return map
}

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

private fun GiantBombClient.DetailsResult.toMap(): Map<String, Any> {
    val map = mutableMapOf(
        "site_detail_url" to siteDetailUrl,
        "name" to name,
        "images" to images
    )
    if (deck != null) {
        map += ("deck" to deck!!)
    }
    if (originalReleaseDate != null) {
        map += ("original_release_date" to "$originalReleaseDate 00:00:00")
    }
    if (image != null) {
        map += ("image" to image!!.toMap())
    }
    if (genres != null) {
        map += ("genres" to genres!!.map { it.toMap() })
    }
    return map.toMap()
}

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
    "id" to rnd.nextInt(100),
    "name" to name,
    "site_detail_url" to randomUrl()
)

private fun GiantBombClient.Status.asString() = this.toString().toUpperCase()