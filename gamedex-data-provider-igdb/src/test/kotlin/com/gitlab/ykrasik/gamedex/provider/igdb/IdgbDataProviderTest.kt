package com.gitlab.ykrasik.gamedex.provider.igdb

import com.github.ykrasik.gamedex.common.datamodel.*
import com.gitlab.ykrasik.gamedex.provider.DataProviderException
import com.gitlab.ykrasik.gamedex.provider.ProviderFetchResult
import com.gitlab.ykrasik.gamedex.provider.ProviderSearchResult
import io.kotlintest.specs.StringSpec
import org.joda.time.LocalDate

/**
 * User: ykrasik
 * Date: 04/12/2017
 * Time: 10:03
 */
class IgdbDataProviderTest : StringSpec() {
    val provider = IgdbDataProvider(IgdbConfig())

    init {
        "Retrieve a single search result" {
            val response = provider.search("no man's sky", GamePlatform.pc)
            response shouldBe listOf(
                ProviderSearchResult(
                    detailUrl = "https://igdbcom-internet-game-database-v1.p.mashape.com/games/3225",
                    name = "No Man's Sky",
                    releaseDate = LocalDate.parse("2016-08-12"),
                    score = 71.75,
                    thumbnailUrl = "http://images.igdb.com/igdb/image/upload/t_thumb/sixpdbypwojsyly22a1l.png"
                )
            )
        }

        "Retrieve multiple search results" {
            val response = provider.search("tItaN QUEST", GamePlatform.pc)
            response shouldBe listOf(
                ProviderSearchResult(
                    detailUrl = "https://igdbcom-internet-game-database-v1.p.mashape.com/games/8311",
                    name = "Titan Quest",
                    releaseDate = LocalDate.parse("2006-06-26"),
                    score = 83.3333333333333,
                    thumbnailUrl = "http://images.igdb.com/igdb/image/upload/t_thumb/yqxdf0umlkvw6iqg0rll.png"
                ),
                ProviderSearchResult(
                    detailUrl = "https://igdbcom-internet-game-database-v1.p.mashape.com/games/8312",
                    name = "Titan Quest: Immortal Throne",
                    releaseDate = LocalDate.parse("2007-03-05"),
                    score = 76.6666666666667,
                    thumbnailUrl = "http://images.igdb.com/igdb/image/upload/t_thumb/ijhkjs299feklpg2ne9f.png"
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
            val thumbnailUrl = "http://images.igdb.com/igdb/image/upload/t_thumb/sixpdbypwojsyly22a1l.png"
            val detailUrl = "https://igdbcom-internet-game-database-v1.p.mashape.com/games/3225"
            val response = provider.fetch(searchResult(detailUrl, name, releaseDate, thumbnailUrl))
            response shouldBe ProviderFetchResult(
                providerData = GameProviderData(
                    type = DataProviderType.Igdb,
                    detailUrl = detailUrl
                ),
                gameData = GameData(
                    name = name,
                    description = "Inspired by the adventure and imagination that we love from classic science-fiction, No Man's Sky presents you with a galaxy to explore, filled with unique planets and lifeforms, and constant danger and action. \n\nIn No Man's Sky, every star is the light of a distant sun, each orbited by planets filled with life, and you can go to any of them you choose. Fly smoothly from deep space to planetary surfaces, with no loading screens, and no limits. In this infinite procedurally generated universe, you'll discover places and creatures that no other players have seen before - and perhaps never will again.",
                    releaseDate = releaseDate,
                    criticScore = 71.75,
                    userScore = 63.6108597785994,
                    genres = listOf("Shooter", "Role-playing (RPG)", "Simulator", "Adventure", "Indie")
                ),
                imageData = GameImageData(
                    thumbnailUrl = thumbnailUrl,
                    posterUrl = "http://images.igdb.com/igdb/image/upload/t_screenshot_huge/sixpdbypwojsyly22a1l.png",
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
                provider.fetch(searchResult("https://igdbcom-internet-game-database-v1.p.mashape.com/games/3225-1"))
            }
            e.message shouldBe "Bad Request"
        }
    }

    private fun searchResult(detailUrl: String,
                             name: String = "",
                             releaseDate: LocalDate? = null,
                             thumbnailUrl: String? = null) = ProviderSearchResult(
        detailUrl = detailUrl, name = name, releaseDate = releaseDate, score = null, thumbnailUrl = thumbnailUrl
    )
}