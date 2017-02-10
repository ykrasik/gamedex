package com.gitlab.ykrasik.gamedex.provider.giantbomb

import com.gitlab.ykrasik.gamedex.common.datamodel.*
import com.gitlab.ykrasik.gamedex.common.exception.GameDexException
import com.gitlab.ykrasik.gamedex.provider.ProviderFetchResult
import com.gitlab.ykrasik.gamedex.provider.ProviderSearchResult
import io.kotlintest.matchers.have
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
            val response = provider.search("no man's sky", GamePlatform.pc)
            response shouldBe listOf(
                ProviderSearchResult(
                    apiUrl = "http://www.giantbomb.com/api/game/3030-44656/",
                    name = "No Man's Sky",
                    releaseDate = LocalDate.parse("2016-08-09"),
                    score = null,
                    thumbnailUrl = "http://www.giantbomb.com/api/image/scale_avatar/2876765-no%20man%27s%20sky%20v5.jpg"
                )
            )
        }

        "Retrieve multiple search results" {
            val response = provider.search("tItaN QUEST", GamePlatform.pc)
            response shouldBe listOf(
                ProviderSearchResult(
                    apiUrl = "http://www.giantbomb.com/api/game/3030-8638/",
                    name = "Titan Quest",
                    releaseDate = LocalDate.parse("2006-06-26"),
                    score = null,
                    thumbnailUrl = "http://www.giantbomb.com/api/image/scale_avatar/301069-titan_quest_pc.jpg"
                ),
                ProviderSearchResult(
                    apiUrl = "http://www.giantbomb.com/api/game/3030-13762/",
                    name = "Titan Quest: Immortal Throne",
                    releaseDate = LocalDate.parse("2007-03-08"),
                    score = null,
                    thumbnailUrl = "http://www.giantbomb.com/api/image/scale_avatar/766079-tqitboxart.jpg"
                )
            )
        }

        "Not find any search results" {
            val response = provider.search("not found", GamePlatform.pc)
            response shouldBe emptyList<ProviderSearchResult>()
        }

        "Fetch a valid game details url" {
            val name = "No Man's Sky"
            val releaseDate = LocalDate.parse("2016-08-09")
            val detailUrl = "http://www.giantbomb.com/api/game/3030-44656/"
            val response = provider.fetch(searchResult(detailUrl, name, releaseDate))
            response shouldBe ProviderFetchResult(
                providerData = ProviderData(
                    type = DataProviderType.GiantBomb,
                    apiUrl = detailUrl,
                    url = "http://www.giantbomb.com/no-mans-sky/3030-44656/"
                ),
                gameData = GameData(
                    name = name,
                    description = "A procedurally generated space exploration game from Hello Games, the creators of Joe Danger.",
                    releaseDate = releaseDate,
                    criticScore = null,
                    userScore = null,
                    genres = listOf("Simulation", "Action-Adventure")
                ),
                imageUrls = ImageUrls(
                    thumbnailUrl = "http://www.giantbomb.com/api/image/scale_avatar/2876765-no%20man%27s%20sky%20v5.jpg",
                    posterUrl = "http://www.giantbomb.com/api/image/scale_large/2876765-no%20man%27s%20sky%20v5.jpg",
                    screenshotUrls = emptyList()
                )
            )
        }

        "Fail to fetch an invalid game details url" {
            val e = shouldThrow<GameDexException> {
                provider.fetch(searchResult("http://www.giantbomb.com/api/game/3030-446567/"))
            }
            e.message shouldBe "Invalid statusCode: ${GiantBombStatus.notFound}"
        }

        "Fail to fetch a non-existing game details url" {
            val e = shouldThrow<GameDexException> {
                provider.fetch(searchResult("http://www.giantbomb.com/api/game/3030-44656-7/"))
            }
            e.message!! should have substring "404"
        }
    }

    private fun searchResult(detailUrl: String, name: String = "", releaseDate: LocalDate? = null) = ProviderSearchResult(
        apiUrl = detailUrl, name = name, releaseDate = releaseDate, score = null, thumbnailUrl = null
    )
}