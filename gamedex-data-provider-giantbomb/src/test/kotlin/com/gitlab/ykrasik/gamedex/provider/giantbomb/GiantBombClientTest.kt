package com.gitlab.ykrasik.gamedex.provider.giantbomb

import com.github.ykrasik.gamedex.datamodel.GamePlatform
import com.gitlab.ykrasik.gamedex.provider.giantbomb.client.*
import io.kotlintest.specs.WordSpec
import org.joda.time.LocalDate

/**
 * User: ykrasik
 * Date: 01/10/2016
 * Time: 21:46
 */
class GiantBombClientTest : WordSpec() {
    val client = GiantBombClient(GiantBombConfig())

    init {
        "GiantBombClient" should {
            "return a single search result" {
                val response = client.search("no man's sky", GamePlatform.PC)
                response shouldBe GiantBombSearchResponse(
                    statusCode = GiantBombStatus.ok,
                    results = listOf(GiantBombSearchResult(
                        apiDetailUrl = "http://www.giantbomb.com/api/game/3030-44656/",
                        name = "No Man's Sky",
                        originalReleaseDate = LocalDate.parse("2016-08-09"),
                        image = GiantBombSearchImage("http://www.giantbomb.com/api/image/scale_avatar/2876765-no%20man%27s%20sky%20v5.jpg")
                    ))
                )
            }

            "return multiple search results" {
                val response = client.search("tItaN QUEST", GamePlatform.PC)
                response shouldBe GiantBombSearchResponse(
                    statusCode = GiantBombStatus.ok,
                    results = listOf(
                        GiantBombSearchResult(
                            apiDetailUrl = "http://www.giantbomb.com/api/game/3030-8638/",
                            name = "Titan Quest",
                            originalReleaseDate = LocalDate.parse("2006-06-26"),
                            image = GiantBombSearchImage("http://www.giantbomb.com/api/image/scale_avatar/301069-titan_quest_pc.jpg")
                        ),
                        GiantBombSearchResult(
                            apiDetailUrl = "http://www.giantbomb.com/api/game/3030-13762/",
                            name = "Titan Quest: Immortal Throne",
                            originalReleaseDate = LocalDate.parse("2007-03-08"),
                            image = GiantBombSearchImage("http://www.giantbomb.com/api/image/scale_avatar/766079-tqitboxart.jpg")
                        ))
                )
            }

            "not find any search results" {
                val response = client.search("not found", GamePlatform.PC)
                response shouldBe GiantBombSearchResponse(
                    statusCode = GiantBombStatus.ok,
                    results = listOf()
                )
            }

            "fetch a valid game details url" {
                val response = client.fetch("http://www.giantbomb.com/api/game/3030-44656/")
                response shouldBe GiantBombDetailsResponse(
                    statusCode = GiantBombStatus.ok,
                    results = listOf(GiantBombDetailsResult(
                        name = "No Man's Sky",
                        deck = "A procedurally generated space exploration game from Hello Games, the creators of Joe Danger.",
                        originalReleaseDate = LocalDate.parse("2016-08-09"),
                        image = GiantBombDetailsImage(
                            thumbUrl = "http://www.giantbomb.com/api/image/scale_avatar/2876765-no%20man%27s%20sky%20v5.jpg",
                            superUrl = "http://www.giantbomb.com/api/image/scale_large/2876765-no%20man%27s%20sky%20v5.jpg"
                        ),
                        genres = listOf(GiantBombGenre("Simulation"), GiantBombGenre("Action-Adventure"))
                    ))
                )
            }

            "fail to fetch an invalid game details url" {
                val response = client.fetch("http://www.giantbomb.com/api/game/3030-446567/")
                response shouldBe GiantBombDetailsResponse(statusCode = GiantBombStatus.notFound, results = emptyList())
            }

            "fail to fetch a non-existing game details url" {
                val response = client.fetch("http://www.giantbomb.com/api/game/3030-44656-7/")
                response shouldBe GiantBombDetailsResponse(statusCode = GiantBombStatus.notFound, results = emptyList())
            }
        }
    }
}