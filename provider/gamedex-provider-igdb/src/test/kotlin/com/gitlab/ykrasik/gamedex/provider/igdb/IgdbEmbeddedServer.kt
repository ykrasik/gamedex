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

package com.gitlab.ykrasik.gamedex.provider.igdb

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder
import com.gitlab.ykrasik.gamedex.plugin.DefaultPlugin
import com.gitlab.ykrasik.gamedex.plugin.PluginDescriptor
import com.gitlab.ykrasik.gamedex.test.*
import com.gitlab.ykrasik.gamedex.util.freePort
import com.gitlab.ykrasik.gamedex.util.toJsonStr
import com.google.inject.Provides
import io.ktor.application.Application
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.request.receiveText
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.routing
import io.ktor.util.pipeline.PipelineContext
import kotlinx.coroutines.delay
import java.io.Closeable
import javax.inject.Singleton

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

    @Suppress("ClassName")
    inner class anyRequest {
        infix fun willReturnSearch(results: List<IgdbClient.SearchResult>) = returns(results)
        infix fun willReturnFetch(response: List<IgdbClient.FetchResult>) = returns(response)

        private fun <T> returns(results: List<T>) {
            wiremock.givenThat(post(urlPathEqualTo("/")).willReturn(aJsonResponse(results)))
        }

        infix fun willFailWith(status: HttpStatusCode) {
            wiremock.givenThat(post(anyUrl()).willReturn(aResponse().withStatus(status.value)))
        }
    }
}

class IgdbFakeServer(port: Int = freePort, private val apiKey: String) : KtorFakeServer(port), GameProviderFakeServer {
    private val imagePath = "images"
    private val baseImageUrl = "$baseUrl/$imagePath"
    private val thumbnailPath = "t_thumb_2x"
    private val posterPath = "t_screenshot_huge"

    override val id = "Igdb"
    override fun randomProviderGameId() = randomInt().toString()
    override val thumbnailUrl = "$baseImageUrl/$thumbnailPath"
    override val posterUrl = "$baseImageUrl/$posterPath"
    override val screenshotUrl = posterUrl

    override fun Application.setupServer() {
        routing {
            post("/") {
                authorized {
                    val body = call.receiveText()
                    delay(50, 800)
                    val response = if (body.contains("search")) {
                        randomSearchResults()
                    } else {
                        randomDetailResponse()
                    }
                    call.respondText(response, ContentType.Application.Json)
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
        if (call.request.headers["user-key"] == apiKey || System.getProperty("gameDex.provider.skipCredentialValidation") != null) {
            body()
        } else {
            call.respond(HttpStatusCode.Unauthorized)
        }
    }

    private suspend fun delay(minMillis: Int, maxMillis: Int) = delay(randomInt(min = minMillis, max = maxMillis).toLong())

    private fun randomSearchResults(): String = randomList(10) {
        IgdbClient.SearchResult(
            id = randomInt(),
            name = randomName(),
            summary = randomParagraph(),
            aggregatedRating = randomScore().score,
            aggregatedRatingCount = randomScore().numReviews,
            rating = randomScore().score,
            ratingCount = randomScore().numReviews,
            releaseDates = randomReleaseDates(),
            cover = randomImage()
        )
    }.toJsonStr()

    private fun randomPlatform() = listOf(6, 9, 12, 48, 49).randomElement()

    private fun randomDetailResponse(): String = listOf(IgdbClient.FetchResult(
        url = randomUrl(),
        name = randomName(),
        summary = randomParagraph(),
        releaseDates = randomReleaseDates(),
        aggregatedRating = randomScore().score,
        aggregatedRatingCount = randomScore().numReviews,
        rating = randomScore().score,
        ratingCount = randomScore().numReviews,
        cover = IgdbClient.Image(imageId = randomWord()),
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

    private fun randomImage() = IgdbClient.Image(imageId = randomWord())

    override fun setupEnv() {
        System.setProperty("gameDex.provider.igdb.baseUrl", baseUrl)
        System.setProperty("gameDex.provider.igdb.baseImageUrl", baseImageUrl)
    }
}

@Suppress("unused")
object IgdbProviderTestkitPlugin : DefaultPlugin() {
    override val descriptor = PluginDescriptor(
        id = "provider.igdb.testkit",
        author = "Yevgeny Krasik",
        version = "0.1.0"
    )

    @Provides
    @Singleton
    fun igdbFakeServerFactory() = object : FakeServerFactory {
        override val preferredPort = 9002
        override fun create(port: Int) = IgdbFakeServer(port, apiKey = "valid")
    }
}