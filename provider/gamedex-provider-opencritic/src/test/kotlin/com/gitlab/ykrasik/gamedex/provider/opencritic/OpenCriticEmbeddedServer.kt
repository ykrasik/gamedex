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

package com.gitlab.ykrasik.gamedex.provider.opencritic

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
import io.ktor.response.*
import io.ktor.routing.*
import kotlinx.coroutines.delay
import java.io.Closeable
import javax.inject.Singleton
import kotlin.random.Random

/**
 * User: ykrasik
 * Date: 28/09/2019
 * Time: 22:15
 */
class OpenCriticMockServer(port: Int = freePort) : Closeable {
    val baseUrl = "http://localhost:$port"
    private val wiremock = WireMockServer(port)

    fun start() = wiremock.start()
    fun reset() = wiremock.resetAll()
    override fun close() = wiremock.stop()

    fun verify(requestPatternBuilder: () -> RequestPatternBuilder) = wiremock.verify(requestPatternBuilder())

    abstract inner class BaseRequest {
        infix fun willFailWith(status: HttpStatusCode) {
            wiremock.givenThat(get(anyUrl()).willReturn(aResponse().withStatus(status.value)))
        }
    }

    @Suppress("ClassName")
    inner class anySearchRequest : BaseRequest() {
        infix fun willReturn(results: List<OpenCriticClient.SearchResult>) {
            wiremock.givenThat(get(urlPathEqualTo("/api/meta/search")).willReturn(aJsonResponse(results)))
        }
    }

    @Suppress("ClassName")
    inner class aFetchRequest(private val id: Int) : BaseRequest() {
        infix fun willReturn(result: OpenCriticClient.FetchResult) {
            wiremock.givenThat(get(urlPathEqualTo("/api/game/$id")).willReturn(aJsonResponse(result)))
        }
    }
}

class OpenCriticFakeServer(port: Int = freePort) : KtorFakeServer(port), GameProviderFakeServer {
    private val thumbnailPath = "images/thumbnail"
    private val screenshotPath = "images/screenshot"

    override val id = OpenCriticProvider.id
    override val supportedPlatforms = OpenCriticProvider.supportedPlatforms

    override fun randomProviderGameId() = randomInt().toString()
    override val thumbnailUrl = "$baseUrl/$thumbnailPath"
    override val posterUrl = null
    override val screenshotUrl = "$baseUrl/$screenshotPath"

    override fun Application.setupServer() {
        routing {
            get("/api/game/search") {
                delay(50, 400)
                call.respondText(randomSearchResults(), ContentType.Application.Json)
            }
            get("/api/game/{id}") {
                delay(50, 400)
                call.respondText(randomDetailResponse(), ContentType.Application.Json)
            }
            get("$thumbnailPath/{imageName}") {
                delay(100, 600)
                call.respond(com.gitlab.ykrasik.gamedex.test.randomImage())
            }
            get("$screenshotPath/{imageName}") {
                delay(200, 800)
                call.respond(com.gitlab.ykrasik.gamedex.test.randomImage())     // TODO: Use a different set of images
            }
        }
    }

    private suspend fun delay(minMillis: Int, maxMillis: Int) = delay(randomInt(min = minMillis, max = maxMillis).toLong())

    private fun randomSearchResults(): String = randomList(10) {
        OpenCriticClient.SearchResult(
            id = randomInt(),
            name = randomName(),
            dist = Random.nextDouble(),
            relation = "game"
        )
    }.toJsonStr()

    private fun randomDetailResponse(): String = OpenCriticClient.FetchResult(
        id = randomInt(),
        name = randomName(),
        description = randomParagraph(),
        medianScore = randomScore().score.sometimesNull() ?: -1.0,
        numReviews = randomScore().numReviews.sometimesNull() ?: 0,
        Genres = randomList(4) {
            OpenCriticClient.Genre(id = randomInt(), name = randomGenre())
        },
        Platforms = listOf(
            OpenCriticClient.Platform(id = randomInt(), shortName = randomWord(), releaseDate = randomDateTime())
        ),
        firstReleaseDate = randomDateTime(),
        verticalLogoScreenshot = randomImage().sometimesNull(),
        bannerScreenshot = randomImage().sometimesNull(),
        logoScreenshot = randomImage().sometimesNull(),
        mastheadScreenshot = randomImage().sometimesNull(),
        screenshots = randomList(10) { randomImage() }.sometimesNull() ?: emptyList()
    ).toJsonStr()

    private fun randomImage() = OpenCriticClient.Image(
        fullRes = "${screenshotUrl.removePrefix("http:")}/${randomWord()}",
        thumbnail = "${thumbnailUrl.removePrefix("http:")}/${randomWord()}"
    )

    override fun setupEnv() {
        System.setProperty("gameDex.provider.opencritic.baseUrl", baseUrl)
    }
}

@Suppress("unused")
object OpenCriticProviderTestkitPlugin : DefaultPlugin() {
    override val descriptor = PluginDescriptor(
        id = "provider.opencritic.testkit",
        author = "Yevgeny Krasik",
        version = "0.1.0"
    )

    @Provides
    @Singleton
    fun opencriticFakeServerFactory() = object : FakeServerFactory {
        override val preferredPort = 9003
        override fun create(port: Int) = OpenCriticFakeServer(port)
    }
}
