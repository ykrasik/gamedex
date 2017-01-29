package com.gitlab.ykrasik.gamedex.provider.giantbomb

import com.github.ykrasik.gamedex.common.datamodel.*
import com.gitlab.ykrasik.gamedex.provider.DataProviderException
import com.gitlab.ykrasik.gamedex.provider.ProviderFetchResult
import com.gitlab.ykrasik.gamedex.provider.ProviderSearchResult
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
                    detailUrl = "http://www.giantbomb.com/api/game/3030-44656/",
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
            val response = provider.search("not found", GamePlatform.pc)
            response shouldBe emptyList<ProviderSearchResult>()
        }

        "Fetch a valid game details url" {
            val name = "No Man's Sky"
            val releaseDate = LocalDate.parse("2016-08-09")
            val detailUrl = "http://www.giantbomb.com/api/game/3030-44656/"
            val response = provider.fetch(searchResult(detailUrl, name, releaseDate))
            response shouldBe ProviderFetchResult(
                providerData = GameProviderData(
                type = DataProviderType.GiantBomb,
                detailUrl = detailUrl
                ),
                gameData = GameData(
                name = name,
                description = "A procedurally generated space exploration game from Hello Games, the creators of Joe Danger.",
                releaseDate = releaseDate,
                criticScore = null,
                userScore = null,
                    genres = listOf("Simulation", "Action-Adventure")
                ),
                imageData = GameImageData(
                thumbnailUrl = "http://www.giantbomb.com/api/image/scale_avatar/2876765-no%20man%27s%20sky%20v5.jpg",
                posterUrl = "http://www.giantbomb.com/api/image/scale_large/2876765-no%20man%27s%20sky%20v5.jpg",
                    screenshot1Url = null,
                    screenshot2Url = null,
                    screenshot3Url = null,
                    screenshot4Url = null,
                    screenshot5Url = null,
                    screenshot6Url = null,
                    screenshot7Url = null,
                    screenshot8Url = null,
                    screenshot9Url = null,
                    screenshot10Url = null
                )
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

    private fun searchResult(detailUrl: String, name: String = "", releaseDate: LocalDate? = null) = ProviderSearchResult(
        detailUrl = detailUrl, name = name, releaseDate = releaseDate, score = null, thumbnailUrl = null
    )
}