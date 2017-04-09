package com.gitlab.ykrasik.gamedex.provider.giantbomb

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.gitlab.ykrasik.gamedex.common.testkit.*
import com.gitlab.ykrasik.gamedex.common.util.toJsonStr
import com.gitlab.ykrasik.gamedex.provider.aJsonResponse
import com.gitlab.ykrasik.gamedex.provider.withQueryParam
import kotlinx.coroutines.experimental.delay
import org.jetbrains.ktor.application.call
import org.jetbrains.ktor.http.ContentType
import org.jetbrains.ktor.netty.embeddedNettyServer
import org.jetbrains.ktor.response.respondText
import org.jetbrains.ktor.routing.get
import org.jetbrains.ktor.routing.routing
import java.io.Closeable
import java.util.concurrent.TimeUnit

/**
 * User: ykrasik
 * Date: 19/03/2017
 * Time: 13:14
 */
class GiantBombEmbeddedServer(port: Int) : Closeable {
    private val baseUrl = "http://localhost:$port"
    private val apiDetailPath = "details"
    private val apiDetailUrl = "$baseUrl/$apiDetailPath"
    private val imagePath = "images"
    private val imageUrl = "$baseUrl/$imagePath"

    private val searchFields = listOf("api_detail_url", "name", "original_release_date", "image").joinToString(",")
    private val fetchDetailsFields = listOf("site_detail_url", "deck", "image", "genres").joinToString(",")

    private val wiremock = WireMockServer(port)
    private val ktor = embeddedNettyServer(port) {
        routing {
            get("/") {
                call.respondText(randomSearchResponse().toMap().toJsonStr(), ContentType.Application.Json)
            }
            get(imagePath) {
                delay(rnd.nextInt(700).toLong(), TimeUnit.MILLISECONDS)
                call.respond(TestImages.randomImageBytes())
            }
            get(apiDetailPath) {
                call.respondText(randomDetailResponse().toMap().toJsonStr(), ContentType.Application.Json)
            }
        }
    }

    private fun randomSearchResponse() = GiantBomb.SearchResponse(
        statusCode = GiantBomb.Status.ok,
        results = List(rnd.nextInt(20)) {
            GiantBomb.SearchResult(
                apiDetailUrl = apiDetailUrl,
                name = randomName(),
                originalReleaseDate = randomLocalDate(),
                image = GiantBomb.SearchImage(
                    thumbUrl = imageUrl
                )
            )
        }
    )

    private fun randomDetailResponse() = GiantBomb.DetailsResponse(
        statusCode = GiantBomb.Status.ok,
        results = listOf(GiantBomb.DetailsResult(
            siteDetailUrl = randomUrl(),
            deck = randomSentence(maxWords = 15),
            image = GiantBomb.DetailsImage(
                thumbUrl = imageUrl,
                superUrl = imageUrl   // TODO: Support once implemented
            ),
            genres = List(rnd.nextInt(4)) {
                GiantBomb.Genre(name = randomString())
            }
        ))
    )

    inner class searchRequest(private val apiKey: String, private val name: String, private val platform: Int) {
        infix fun willReturn(response: GiantBomb.SearchResponse) {
            wiremock.givenThat(aSearchRequest.willReturn(aJsonResponse(response.toMap())))
        }

        infix fun willFailWith(status: Int) {
            wiremock.givenThat(aSearchRequest.willReturn(aResponse().withStatus(status)))
        }

        private val aSearchRequest get() = get(urlPathEqualTo("/"))
            .withQueryParam("api_key", apiKey)
            .withQueryParam("format", "json")
            .withQueryParam("filter", "name:$name,platforms:$platform")
            .withQueryParam("field_list", searchFields)
    }

    inner class fetchRequest(private val apiKey: String, private val apiUrl: String) {
        infix fun willReturn(response: GiantBomb.DetailsResponse) {
            wiremock.givenThat(aFetchRequest.willReturn(aJsonResponse(response.toMap())))
        }

        infix fun willFailWith(status: Int) {
            wiremock.givenThat(aFetchRequest.willReturn(aResponse().withStatus(status)))
        }

        private val aFetchRequest get() = get(urlPathEqualTo("/$apiUrl"))
            .withQueryParam("api_key", apiKey)
            .withQueryParam("field_list", fetchDetailsFields)
    }

    private fun GiantBomb.SearchResponse.toMap() = mapOf(
        "error" to statusCode.asString(),
        "limit" to 100,
        "offset" to 0,
        "number_of_page_results" to 1,
        "number_of_total_results" to 1,
        "status_code" to statusCode.key,
        "results" to results.map { it.toMap() },
        "version" to "1.0"
    )

    private fun GiantBomb.SearchResult.toMap(): Map<String, Any> {
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

    private fun GiantBomb.SearchImage.toMap() = mapOf(
        "icon_url" to randomUrl(),
        "medium_url" to randomUrl(),
        "screen_url" to randomUrl(),
        "small_url" to randomUrl(),
        "super_url" to randomUrl(),
        "thumb_url" to thumbUrl,
        "tiny_url" to randomUrl()
    )

    private fun GiantBomb.DetailsResponse.toMap() = mapOf(
        "error" to statusCode.asString(),
        "limit" to 1,
        "offset" to 0,
        "number_of_page_results" to 1,
        "number_of_total_results" to 1,
        "status_code" to statusCode.key,
        "results" to if (results.isNotEmpty()) results.first().toMap() else emptyList<Any>(),
        "version" to "1.0"
    )

    private fun GiantBomb.DetailsResult.toMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "site_detail_url" to siteDetailUrl
        )
        if (deck != null) {
            map += ("deck" to deck!!)
        }
        if (image != null) {
            map += ("image" to image!!.toMap())
        }
        if (genres != null) {
            map += ("genres" to genres!!.map { it.toMap() })
        }
        return map.toMap()
    }

    private fun GiantBomb.DetailsImage.toMap() = mapOf(
        "icon_url" to randomUrl(),
        "medium_url" to randomUrl(),
        "screen_url" to randomUrl(),
        "small_url" to randomUrl(),
        "super_url" to superUrl,
        "thumb_url" to thumbUrl,
        "tiny_url" to randomUrl()
    )

    private fun GiantBomb.Genre.toMap() = mapOf(
        "api_detail_url" to randomUrl(),
        "id" to rnd.nextInt(100),
        "name" to name,
        "site_detail_url" to randomUrl()
    )

    private fun GiantBomb.Status.asString() = this.toString().toUpperCase()

    fun start(isFakeProd: Boolean = false): GiantBombEmbeddedServer = apply {
        if (isFakeProd) {
            ktor.start()
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