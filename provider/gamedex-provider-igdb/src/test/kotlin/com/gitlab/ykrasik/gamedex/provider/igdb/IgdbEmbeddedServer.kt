/****************************************************************************
 * Copyright (C) 2016-2020 Yevgeny Krasik                                   *
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
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.pipeline.*
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
    val oauthPath = "oauth"
    val oauthUrl = "$baseUrl/$oauthPath"
    private val wiremock = WireMockServer(port)

    fun start() = wiremock.start()
    fun reset() = wiremock.resetAll()
    override fun close() = wiremock.stop()

    fun verify(requestPatternBuilder: () -> RequestPatternBuilder) = wiremock.verify(requestPatternBuilder())

    @Suppress("ClassName")
    inner class oauthRequest {
        fun willReturn(token: String, expiresInSeconds: Int) =
            wiremock.givenThat(post(urlPathEqualTo("/oauth"))
                .willReturn(aJsonResponse(mapOf("access_token" to token, "expires_in" to expiresInSeconds))))

        infix fun willFailWith(status: HttpStatusCode) {
            wiremock.givenThat(post(urlPathEqualTo("/oauth")).willReturn(aResponse().withStatus(status.value)))
        }
    }

    @Suppress("ClassName")
    inner class anyRequest {
        infix fun willReturnSearch(results: List<IgdbClient.SearchResult>) = returns(results)
        infix fun willReturnFetch(response: List<IgdbClient.FetchResult>) = returns(response)

        private fun <T> returns(results: List<T>) {
            wiremock.givenThat(post(urlPathEqualTo("/")).willReturn(aJsonResponse(results)))
        }

        infix fun willFailWith(status: HttpStatusCode) {
            wiremock.givenThat(post(urlPathEqualTo("/")).willReturn(aResponse().withStatus(status.value)))
        }
    }
}

class IgdbFakeServer(port: Int = freePort, private val apiKey: String) : KtorFakeServer(port), GameProviderFakeServer {
    private val imagePath = "images"
    private val baseImageUrl = "$baseUrl/$imagePath"
    private val thumbnailPath = "t_thumb_2x"
    private val posterPath = "t_screenshot_huge"

    override val id = IgdbProvider.id
    override val supportedPlatforms = IgdbProvider.supportedPlatforms
    override fun randomProviderGameId() = randomInt().toString()
    override val thumbnailUrl = "$baseImageUrl/$thumbnailPath"
    override val posterUrl = "$baseImageUrl/$posterPath"
    override val screenshotUrl = posterUrl

    override fun Application.setupServer() {
        System.setProperty("gameDex.provider.igdb.oauthUrl", "$baseUrl/oauth")
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
            post("/oauth") {
                call.respondText(IgdbClient.OAuthResponse(accessToken = "accessToken", expiresIn = 999999999).toJsonStr(), ContentType.Application.Json)
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
            summary = randomParagraph().sometimesNull(),
            aggregatedRating = randomScore().score.sometimesNull(),
            aggregatedRatingCount = randomScore().numReviews,
            rating = randomScore().score.sometimesNull(),
            ratingCount = randomScore().numReviews,
            releaseDates = randomReleaseDates().sometimesNull(),
            firstReleaseDate = randomDateTime().millis.sometimesNull(),
            cover = randomImage().sometimesNull()
        )
    }.toJsonStr()

    private fun randomPlatform() = listOf(6, 9, 12, 48, 49).randomElement()

    private fun randomDetailResponse(): String = listOf(
        IgdbClient.FetchResult(
            url = randomUrl(),
            name = randomName(),
            summary = randomParagraph().sometimesNull(),
            releaseDates = randomReleaseDates().sometimesNull(),
            firstReleaseDate = randomDateTime().millis.sometimesNull(),
            aggregatedRating = randomScore().score.sometimesNull(),
            aggregatedRatingCount = randomScore().numReviews,
            rating = randomScore().score.sometimesNull(),
            ratingCount = randomScore().numReviews,
            cover = IgdbClient.Image(imageId = randomWord()).sometimesNull(),
            screenshots = randomList(10) { randomImage() }.sometimesNull(),
            genres = randomList(4) {
                listOf(2, 4, 5, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 24, 25, 26, 30, 31, 32, 33).randomElement()
            }.sometimesNull()
        )
    ).toJsonStr()

    private fun randomReleaseDates() = randomList(3) { randomReleaseDate() }

    private fun randomReleaseDate(): IgdbClient.ReleaseDate {
        return IgdbClient.ReleaseDate(
            platform = randomPlatform(),
            date = randomDateTime().millis.sometimesNull()
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