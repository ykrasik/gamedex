package com.gitlab.ykrasik.gamedex.provider.igdb

import com.gitlab.ykrasik.gamedex.*
import com.gitlab.ykrasik.gamedex.test.ScopedWordSpec
import com.gitlab.ykrasik.gamedex.util.appConfig
import io.kotlintest.matchers.shouldBe
import org.joda.time.LocalDate

/**
 * User: ykrasik
 * Date: 12/04/2017
 * Time: 10:03
 */
class IgdbProviderRealIT : ScopedWordSpec() {
    val config = IgdbConfig(appConfig)
    val provider = IgdbDataProvider(config, IgdbClient(config))

    init {
        "IgdbDataProvider" should {
            "search & retrieve a single search result" {
                provider.search(name, Platform.pc) shouldBe listOf(ProviderSearchResult(
                    apiUrl = apiUrl,
                    name = name,
                    releaseDate = releaseDate,
                    score = criticScore,
                    thumbnailUrl = thumbnailUrl
                ))
            }

            "fetch game details" {
                provider.fetch(apiUrl, Platform.pc) shouldBe RawGameData(
                    providerData = ProviderData(
                        type = GameProviderType.Igdb,
                        apiUrl = apiUrl,
                        siteUrl = url
                    ),
                    gameData = GameData(
                        name = name,
                        description = description,
                        releaseDate = releaseDate,
                        criticScore = criticScore,
                        userScore = userScore,
                        genres = genres
                    ),
                    imageUrls = ImageUrls(
                        thumbnailUrl = thumbnailUrl,
                        posterUrl = posterUrl,
                        screenshotUrls = screenshotUrls
                    )
                )
            }
        }
    }

    val name = "No Man's Sky"
    val apiUrl = "https://igdbcom-internet-game-database-v1.p.mashape.com/games/3225"
    val releaseDate = LocalDate.parse("2016-08-12")
    val criticScore = 72.6428571428571
    val userScore = 63.0478057301822
    val thumbnailUrl = "http://images.igdb.com/igdb/image/upload/t_thumb_2x/sixpdbypwojsyly22a1l.png"
    val posterUrl = "http://images.igdb.com/igdb/image/upload/t_screenshot_huge/sixpdbypwojsyly22a1l.png"
    val url = "https://www.igdb.com/games/no-man-s-sky"
    val description = "Inspired by the adventure and imagination that we love from classic science-fiction, No Man's Sky presents you with a galaxy to explore, filled with unique planets and lifeforms, and constant danger and action. \n\nIn No Man's Sky, every star is the light of a distant sun, each orbited by planets filled with life, and you can go to any of them you choose. Fly smoothly from deep space to planetary surfaces, with no loading screens, and no limits. In this infinite procedurally generated universe, you'll discover places and creatures that no other players have seen before - and perhaps never will again."
    val genres = listOf("Shooter", "Role-playing (RPG)", "Simulator", "Adventure", "Indie")
    val screenshotUrls = listOf(
        "http://images.igdb.com/igdb/image/upload/t_screenshot_huge/slbfwlrvqjrgvrvfghrk.png",
        "http://images.igdb.com/igdb/image/upload/t_screenshot_huge/xufkotfn5udk2aijb6f0.png",
        "http://images.igdb.com/igdb/image/upload/t_screenshot_huge/kmyailnbyy0afbyqdfxn.png",
        "http://images.igdb.com/igdb/image/upload/t_screenshot_huge/byull3k2xzfndgivkdlw.png",
        "http://images.igdb.com/igdb/image/upload/t_screenshot_huge/o0pqdynpsv7vvziaxwzr.png"
    )
}