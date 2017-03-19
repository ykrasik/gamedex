package com.gitlab.ykrasik.gamedex.provider.giantbomb

import com.gitlab.ykrasik.gamedex.common.testkit.*
import org.jetbrains.ktor.application.call
import org.jetbrains.ktor.http.ContentType
import org.jetbrains.ktor.netty.embeddedNettyServer
import org.jetbrains.ktor.response.respondText
import org.jetbrains.ktor.routing.get
import org.jetbrains.ktor.routing.routing

/**
 * User: ykrasik
 * Date: 19/03/2017
 * Time: 13:14
 */
class GiantBombEmbeddedServer(port: Int) {
    private val baseUrl = "http://localhost:$port"
    private val apiDetailPath = "details"
    private val imagePath = "images"

    fun start() {
        server.start(wait = false)
    }

    private val server = embeddedNettyServer(port) {
        routing {
            get("/") {
                call.respondText(randomSearchResponse(), ContentType.Application.Json)
            }
            get(imagePath) {
                call.respond(TestImages.randomImageBytes())
            }
            get(apiDetailPath) {
                call.respondText(randomDetailResponse(), ContentType.Application.Json)
            }
        }
    }

    private fun randomSearchResponse(): String = GiantBombSearchResponse(
        statusCode = GiantBombStatus.ok,
        results = List(rnd.nextInt(20)) {
            GiantBombSearchResult(
                apiDetailUrl = "$baseUrl/$apiDetailPath/",
                name = randomName(),
                originalReleaseDate = randomLocalDate(),
                image = GiantBombSearchImage(
                    thumbUrl = "$baseUrl/$imagePath/"
                )
            )
        }
    ).toJson()

    private fun GiantBombSearchResponse.toJson(): String = """{
        "error": "OK",
        "limit": 100,
        "offset": 0,
        "number_of_page_results": 1,
        "number_of_total_results": 1,
        "status_code": ${statusCode.key},
        "results": ${results.toJson { it.toJson() }},
        "version": "1.0"
    }"""

    private fun GiantBombSearchResult.toJson(): String = """{
        "api_detail_url": "$apiDetailUrl",
        "image": ${image.toJson()},
        "name": "$name",
        "original_release_date": "$originalReleaseDate 00:00:00"
    }"""

    private fun GiantBombSearchImage.toJson() = """{
        "icon_url": "${randomUrl()}",
        "medium_url": "${randomUrl()}",
        "screen_url": "${randomUrl()}",
        "small_url": "${randomUrl()}",
        "super_url": "${randomUrl()}",
        "thumb_url": "$thumbUrl",
        "tiny_url": "${randomUrl()}"
    }"""

    private fun randomDetailResponse(): String = GiantBombDetailsResponse(
        statusCode = GiantBombStatus.ok,
        results = listOf(GiantBombDetailsResult(
            siteDetailUrl = randomUrl(),
            deck = randomSentence(maxWords = 15),
            image = GiantBombDetailsImage(
                thumbUrl = "$baseUrl/$imagePath/",
                superUrl = "$baseUrl/$imagePath/"   // TODO: Support once implemented
            ),
            genres = List(rnd.nextInt(4)) {
                GiantBombGenre(name = randomString())
            }
        ))
    ).toJson()

    private fun GiantBombDetailsResponse.toJson() = """{
        "error": "OK",
        "limit": 1,
        "offset": 0,
        "number_of_page_results": 1,
        "number_of_total_results": 1,
        "status_code": ${statusCode.key},
        "results": ${results.first().toJson()},
        "version": "1.0"
    }"""

    private fun GiantBombDetailsResult.toJson() = """{
        "deck": "$deck",
        "image": ${image.toJson()},
        "site_detail_url": "$siteDetailUrl",
        "genres": ${genres.toJson { it.toJson() }}
    }"""

    private fun GiantBombDetailsImage.toJson() = """{
        "icon_url": "${randomUrl()}",
        "medium_url": "${randomUrl()}",
        "screen_url": "${randomUrl()}",
        "small_url": "${randomUrl()}",
        "super_url": "$superUrl",
        "thumb_url": "$thumbUrl",
        "tiny_url": "${randomUrl()}"
    }"""

    private fun GiantBombGenre.toJson(): String = """{
        "api_detail_url": "${randomUrl()}",
        "id": ${rnd.nextInt(100)},
        "name": "$name",
        "site_detail_url": "${randomUrl()}"
    }"""

    private fun <T> List<T>.toJson(transform: (T) -> CharSequence): String =
        joinToString(separator = ",", prefix = "[", postfix = "]", transform = transform)

    // TODO: Allow configuring responses for ITs
}