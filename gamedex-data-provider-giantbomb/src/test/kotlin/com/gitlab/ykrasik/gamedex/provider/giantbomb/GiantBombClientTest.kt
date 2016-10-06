package com.gitlab.ykrasik.gamedex.provider.giantbomb

import com.carlosbecker.guice.GuiceModules
import com.carlosbecker.guice.GuiceTestRunner
import com.github.ykrasik.gamedex.common.module.CommonModule
import com.github.ykrasik.gamedex.datamodel.GamePlatform
import com.gitlab.ykrasik.gamedex.provider.giantbomb.client.*
import com.gitlab.ykrasik.gamedex.provider.giantbomb.module.GiantBombProviderModule
import com.gitlab.ykrasik.gamedex.provider.module.JacksonModule
import org.joda.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

/**
 * User: ykrasik
 * Date: 01/10/2016
 * Time: 21:46
 */
@RunWith(GuiceTestRunner::class)
@GuiceModules(GiantBombProviderModule::class, JacksonModule::class, CommonModule::class)
class GiantBombClientTest {
    @Inject
    lateinit var client: GiantBombClient

    @Test
    fun searchFoundSingleResult() {
        val response = client.search("no man's sky", GamePlatform.PC)
        val expected = GiantBombSearchResponse(
            statusCode = GiantBombStatus.ok,
            results = listOf(GiantBombSearchResult(
                apiDetailUrl = "http://www.giantbomb.com/api/game/3030-44656/",
                name = "No Man's Sky",
                originalReleaseDate = LocalDate.parse("2016-08-09"),
                image = GiantBombSearchImage("http://www.giantbomb.com/api/image/scale_avatar/2876765-no%20man%27s%20sky%20v5.jpg")
            ))
        )
        assertEquals(expected, response)
    }

    @Test
    fun searchFoundMultipleResults() {
        val response = client.search("tItaN QUEST", GamePlatform.PC)
        val expected = GiantBombSearchResponse(
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
        assertEquals(expected, response)
    }

    @Test
    fun searchNotFound() {
        val response = client.search("not found", GamePlatform.PC)
        val expected = GiantBombSearchResponse(
            statusCode = GiantBombStatus.ok,
            results = listOf()
        )
        assertEquals(expected, response)
    }

    @Test
    fun fetchFound() {
        val response = client.fetch("http://www.giantbomb.com/api/game/3030-44656/")
        val expected = GiantBombDetailsResponse(
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
        assertEquals(expected, response)
    }

    @Test
    fun fetchNotFound() {
        val response = client.fetch("http://www.giantbomb.com/api/game/3030-446567/")
        val expected = GiantBombDetailsResponse(statusCode = GiantBombStatus.notFound, results = emptyList())
        assertEquals(expected, response)
    }

    @Test
    fun fetch404() {
        val response = client.fetch("http://www.giantbomb.com/api/game/3030-44656-7/")
        val expected = GiantBombDetailsResponse(statusCode = GiantBombStatus.notFound, results = emptyList())
        assertEquals(expected, response)
    }
}