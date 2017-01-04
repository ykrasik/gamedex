package com.gitlab.ykrasik.gamedex.provider.giantbomb

import com.github.ykrasik.gamedex.datamodel.DataProviderType
import com.github.ykrasik.gamedex.datamodel.GamePlatform
import com.gitlab.ykrasik.gamedex.provider.DataProviderException
import com.gitlab.ykrasik.gamedex.provider.ProviderFetchResult
import com.gitlab.ykrasik.gamedex.provider.ProviderSearchResult
import com.gitlab.ykrasik.gamedex.provider.giantbomb.jackson.GiantBombStatus
import io.kotlintest.specs.StringSpec
import org.joda.time.LocalDate

/**
 * User: ykrasik
 * Date: 08/10/2016
 * Time: 17:59
 */
class GiantBombDataProviderTest : StringSpec() {
    val provider = GiantBombDataProvider(GiantBombConfig())

    init {
        "Retrieve a single search result" {
            val response = provider.search("no man's sky", GamePlatform.PC)
            response shouldBe listOf(
                ProviderSearchResult(
                    detailUrl = "http://www.giantbomb.com/api/game/3030-44656/",
                    name = "No Man's Sky",
                    releaseDate = LocalDate.parse("2016-08-09"),
                    score = null,
                    thumbnailUrl = "http://www.giantbomb.com/api/image/scale_avatar/2876765-no%20man%27s%20sky%20v5.jpg"
                )
            )
        }

        "Retrieve multiple search results" {
            val response = provider.search("tItaN QUEST", GamePlatform.PC)
            response shouldBe listOf(
                ProviderSearchResult(
                    detailUrl = "http://www.giantbomb.com/api/game/3030-8638/",
                    name = "Titan Quest",
                    releaseDate = LocalDate.parse("2006-06-26"),
                    score = null,
                    thumbnailUrl = "http://www.giantbomb.com/api/image/scale_avatar/301069-titan_quest_pc.jpg"
                ),
                ProviderSearchResult(
                    detailUrl = "http://www.giantbomb.com/api/game/3030-13762/",
                    name = "Titan Quest: Immortal Throne",
                    releaseDate = LocalDate.parse("2007-03-08"),
                    score = null,
                    thumbnailUrl = "http://www.giantbomb.com/api/image/scale_avatar/766079-tqitboxart.jpg"
                )
            )
        }

        "Not find any search results" {
            val response = provider.search("not found", GamePlatform.PC)
            response shouldBe emptyList<ProviderSearchResult>()
        }

        "Fetch a valid game details url" {
            val detailUrl = "http://www.giantbomb.com/api/game/3030-44656/"
            val response = provider.fetch(searchResult(detailUrl))
            response shouldBe ProviderFetchResult(
                type = DataProviderType.GiantBomb,
                detailUrl = detailUrl,

                name = "No Man's Sky",
                description = "A procedurally generated space exploration game from Hello Games, the creators of Joe Danger.",
                releaseDate = LocalDate.parse("2016-08-09"),
                criticScore = null,
                userScore = null,

                thumbnailUrl = "http://www.giantbomb.com/api/image/scale_avatar/2876765-no%20man%27s%20sky%20v5.jpg",
                posterUrl = "http://www.giantbomb.com/api/image/scale_large/2876765-no%20man%27s%20sky%20v5.jpg",
                genres = listOf("Simulation", "Action-Adventure")
            )
        }

        "Fail to fetch an invalid game details url" {
            val e = shouldThrow<DataProviderException> {
                provider.fetch(searchResult("http://www.giantbomb.com/api/game/3030-446567/"))
            }
            e.message shouldBe "Invalid statusCode: ${GiantBombStatus.notFound}"
        }

        "Fail to fetch a non-existing game details url" {
            val e = shouldThrow<DataProviderException> {
                provider.fetch(searchResult("http://www.giantbomb.com/api/game/3030-44656-7/"))
            }
            e.message shouldBe "Invalid statusCode: ${GiantBombStatus.notFound}"
        }
    }

    private fun searchResult(detailUrl: String) = ProviderSearchResult(
        detailUrl = detailUrl, name = "", releaseDate = null, score = null, thumbnailUrl = null
    )
}