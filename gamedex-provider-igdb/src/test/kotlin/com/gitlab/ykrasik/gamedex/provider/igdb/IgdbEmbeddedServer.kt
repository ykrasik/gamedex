package com.gitlab.ykrasik.gamedex.provider.igdb

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder
import com.gitlab.ykrasik.gamedex.test.*
import com.gitlab.ykrasik.gamedex.util.toJsonStr
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
class IgdbMockServer(port: Int) : Closeable {
    private val wiremock = WireMockServer(port)

    fun start() = wiremock.start()
    fun reset() = wiremock.resetAll()
    override fun close() = wiremock.stop()

    fun verify(requestPatternBuilder: RequestPatternBuilder) = wiremock.verify(requestPatternBuilder)

    inner abstract class BaseRequest {
        infix fun willFailWith(status: Int) {
            wiremock.givenThat(get(anyUrl()).willReturn(aResponse().withStatus(status)))
        }
    }

    inner class anySearchRequest : BaseRequest() {
        infix fun willReturn(results: List<Igdb.SearchResult>) {
            wiremock.givenThat(get(urlPathEqualTo("/")).willReturn(aJsonResponse(results)))
        }
    }

    inner class aFetchRequest(private val gameId: Int) : BaseRequest() {
        // TODO: This style of 'willReturn' does not allow testing parsing failures.
        infix fun willReturn(response: Igdb.DetailsResult) {
            wiremock.givenThat(get(urlPathEqualTo("/$gameId")).willReturn(aJsonResponse(listOf(response))))
        }
    }
}

class IgdbFakeServer(port: Int) : Closeable {
    private val apiDetailPath = 1

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
            get("images/t_thumb_2x/{imageName}") {
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
        releaseDates = randomReleaseDates(),
        cover = Igdb.Image(
            cloudinaryId = randomString()
        )
    ) }.toJsonStr()

    private fun randomPlatform() = listOf(6, 9, 12, 48, 49).randomElement()

    private fun randomDetailResponse(): String = listOf(Igdb.DetailsResult(
        url = randomUrl(),
        name = randomName(),
        summary = randomSentence(maxWords = 50),
        releaseDates = randomReleaseDates(),
        aggregatedRating = randomScore(),
        rating = randomScore(),
        cover = Igdb.Image(cloudinaryId = randomString()),
        screenshots = List(rnd.nextInt(10)) { Igdb.Image(cloudinaryId = randomString()) },
        genres = List(rnd.nextInt(4)) {
            listOf(2, 4, 5, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 24, 25, 26, 30, 31, 32, 33).randomElement()
        }
    )).toJsonStr()

    private fun randomReleaseDates() = List(rnd.nextInt(4)) { randomReleaseDate() }

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

    fun start() = apply {
        ktor.start(wait = false)
    }

    override fun close() {
        ktor.stop(gracePeriod = 100, timeout = 100, timeUnit = TimeUnit.MILLISECONDS)
    }
}