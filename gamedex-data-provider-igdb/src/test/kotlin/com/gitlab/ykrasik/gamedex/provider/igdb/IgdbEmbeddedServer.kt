package com.gitlab.ykrasik.gamedex.provider.igdb

import com.gitlab.ykrasik.gamedex.common.testkit.*
import com.gitlab.ykrasik.gamedex.common.util.toJsonStr
import kotlinx.coroutines.experimental.delay
import org.jetbrains.ktor.application.call
import org.jetbrains.ktor.http.ContentType
import org.jetbrains.ktor.http.HttpMethod
import org.jetbrains.ktor.netty.embeddedNettyServer
import org.jetbrains.ktor.response.respondText
import org.jetbrains.ktor.routing.*
import java.util.concurrent.TimeUnit

/**
 * User: ykrasik
 * Date: 19/03/2017
 * Time: 16:48
 */
class IgdbEmbeddedServer(port: Int) {
    private val apiDetailPath = 1
    private val image = "image"

    fun start() {
        server.start(wait = false)
    }

    private val server = embeddedNettyServer(port) {
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

    private fun randomSearchResults(name: String): String = List(rnd.nextInt(20)) { IgdbSearchResult(
        id = apiDetailPath,
        name = "$name ${randomName()}",
        aggregatedRating = randomScore(),
        releaseDates = List(rnd.nextInt(4)) { randomReleaseDate() },
        cover = IgdbImage(
            cloudinaryId = image
        )
    ) }.toJsonStr()

    private fun randomReleaseDate(): IgdbReleaseDate {
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
        return IgdbReleaseDate(
            platform = randomPlatform(),
            category = category,
            human = human
        )
    }

    private fun randomPlatform() = listOf(6, 9, 12, 48, 49).randomElement()

    private fun randomDetailResponse(): String = listOf(IgdbDetailsResult(
        url = randomUrl(),
        summary = randomSentence(maxWords = 50),
        aggregatedRating = randomScore(),
        rating = randomScore(),
        cover = IgdbImage(cloudinaryId = image),
        screenshots = List(rnd.nextInt(10)) { IgdbImage(cloudinaryId = image) },
        genres = List(rnd.nextInt(4)) {
            listOf(2, 4, 5, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 24, 25, 26, 30, 31, 32, 33).randomElement()
        }
    )).toJsonStr()

    // TODO: Allow configuring responses for ITs
}