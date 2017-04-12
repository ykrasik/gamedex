package com.gitlab.ykrasik.gamedex.provider.igdb

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder
import com.gitlab.ykrasik.gamedex.common.testkit.*
import com.gitlab.ykrasik.gamedex.common.util.toJsonStr
import com.gitlab.ykrasik.provider.testkit.aJsonResponse
import kotlinx.coroutines.experimental.delay
import org.jetbrains.ktor.application.call
import org.jetbrains.ktor.host.embeddedServer
import org.jetbrains.ktor.http.ContentType
import org.jetbrains.ktor.http.HttpMethod
import org.jetbrains.ktor.netty.Netty
import org.jetbrains.ktor.response.respondText
import org.jetbrains.ktor.routing.*
import java.io.Closeable
import java.util.concurrent.TimeUnit

/**
 * User: ykrasik
 * Date: 19/03/2017
 * Time: 16:48
 */
class IgdbMockServer(port: Int) : Closeable {
    private val wiremock = WireMockServer(port)

    fun start(): Unit = wiremock.start()
    fun reset(): Unit = wiremock.resetAll()
    override fun close(): Unit = wiremock.stop()

    fun verify(requestPatternBuilder: RequestPatternBuilder) = wiremock.verify(requestPatternBuilder)

    inner abstract class BaseRequest {
        infix fun willFailWith(status: Int) {
            wiremock.givenThat(get(anyUrl()).willReturn(aResponse().withStatus(status)))
        }
    }

    inner class aSearchRequest : BaseRequest() {
        infix fun willReturn(results: List<Igdb.SearchResult>) {
            wiremock.givenThat(get(urlPathEqualTo("/")).willReturn(aJsonResponse(results)))
        }
    }

    inner class aFetchRequest(private val gameId: Int) : BaseRequest() {
        infix fun willReturn(response: Igdb.DetailsResult) {
            wiremock.givenThat(get(urlPathEqualTo("/$gameId")).willReturn(aJsonResponse(listOf(response))))
        }
    }
}

class IgdbFakeServer(port: Int) : Closeable {
    private val apiDetailPath = 1
    private val image = "image"

    private val ktor = embeddedServer(Netty, port) {
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

    private fun randomSearchResults(name: String): String = List(rnd.nextInt(20)) { Igdb.SearchResult(
        id = apiDetailPath,
        name = "$name ${randomName()}",
        aggregatedRating = randomScore(),
        releaseDates = List(rnd.nextInt(4)) { randomReleaseDate() },
        cover = Igdb.Image(
            cloudinaryId = image
        )
    ) }.toJsonStr()

    private fun randomReleaseDate(): Igdb.ReleaseDate {
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
        return Igdb.ReleaseDate(
            platform = randomPlatform(),
            category = category,
            human = human
        )
    }

    private fun randomPlatform() = listOf(6, 9, 12, 48, 49).randomElement()

    private fun randomDetailResponse(): String = listOf(Igdb.DetailsResult(
        url = randomUrl(),
        summary = randomSentence(maxWords = 50),
        rating = randomScore(),
        cover = Igdb.Image(cloudinaryId = image),
        screenshots = List(rnd.nextInt(10)) { Igdb.Image(cloudinaryId = image) },
        genres = List(rnd.nextInt(4)) {
            listOf(2, 4, 5, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 24, 25, 26, 30, 31, 32, 33).randomElement()
        }
    )).toJsonStr()

    fun start() = apply {
        ktor.start(wait = false)
    }

    override fun close() {
        ktor.stop(gracePeriod = 100, timeout = 100, timeUnit = TimeUnit.MILLISECONDS)
    }
}