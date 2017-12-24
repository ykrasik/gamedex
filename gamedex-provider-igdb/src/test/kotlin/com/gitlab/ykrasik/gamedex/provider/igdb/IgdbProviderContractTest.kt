package com.gitlab.ykrasik.gamedex.provider.igdb

import com.gitlab.ykrasik.gamedex.*
import com.gitlab.ykrasik.gamedex.test.ScopedWordSpec
import com.gitlab.ykrasik.gamedex.test.assertScore
import com.gitlab.ykrasik.gamedex.util.appConfig
import io.kotlintest.matchers.haveSize
import io.kotlintest.matchers.should
import io.kotlintest.matchers.shouldBe

/**
 * User: ykrasik
 * Date: 12/04/2017
 * Time: 10:03
 */
class IgdbProviderContractTest : ScopedWordSpec() {
    val config = IgdbConfig(appConfig)
    val provider = IgdbProvider(config, IgdbClient(config))

    init {
        "IgdbProvider" should {
            "search & retrieve a single search result" {
                val results = provider.search(name, Platform.pc)
                results should haveSize(1)

                val result = results.first()
                result shouldBe ProviderSearchResult(
                    apiUrl = apiUrl,
                    name = name,
                    releaseDate = releaseDate,
                    criticScore = result.criticScore.verifiedCriticScore,
                    userScore = result.userScore.verifiedUserScore,
                    thumbnailUrl = thumbnailUrl
                )
            }

            "download game details" {
                val result = provider.download(apiUrl, Platform.pc)
                result shouldBe ProviderData(
                    header = ProviderHeader(
                        id = "Igdb",
                        apiUrl = apiUrl,
                        updateDate = nowMock
                    ),
                    gameData = GameData(
                        siteUrl = url,
                        name = name,
                        description = description,
                        releaseDate = releaseDate,
                        criticScore = result.gameData.criticScore.verifiedCriticScore,
                        userScore = result.gameData.userScore.verifiedUserScore,
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
    val apiUrl = "https://api-2445582011268.apicast.io/games/3225"
    val releaseDate = "2016-08-12"
    val Score?.verifiedCriticScore get() = assertScore(min = 72, max = 74, numReviews = 30)
    val Score?.verifiedUserScore get() = assertScore(min = 62, max = 66, numReviews = 80)
    val thumbnailUrl = "http://images.igdb.com/igdb/image/upload/t_thumb_2x/sixpdbypwojsyly22a1l.jpg"
    val posterUrl = "http://images.igdb.com/igdb/image/upload/t_screenshot_huge/sixpdbypwojsyly22a1l.jpg"
    val url = "https://www.igdb.com/games/no-man-s-sky"
    val description =
        "Inspired by the adventure and imagination that we love from classic science-fiction, No Man's Sky presents you with a galaxy to explore, filled with unique planets and lifeforms, and constant danger and action. \n \nIn No Man's Sky, every star is the light of a distant sun, each orbited by planets filled with life, and you can go to any of them you choose. Fly smoothly from deep space to planetary surfaces, with no loading screens, and no limits. In this infinite procedurally generated universe, you'll discover places and creatures that no other players have seen before - and perhaps never will again."
    val genres = listOf("Shooter", "Role-Playing Game (RPG)", "Simulation", "Adventure", "Indie")
    val screenshotUrls = listOf(
        "http://images.igdb.com/igdb/image/upload/t_screenshot_huge/slbfwlrvqjrgvrvfghrk.jpg",
        "http://images.igdb.com/igdb/image/upload/t_screenshot_huge/xufkotfn5udk2aijb6f0.jpg",
        "http://images.igdb.com/igdb/image/upload/t_screenshot_huge/kmyailnbyy0afbyqdfxn.jpg",
        "http://images.igdb.com/igdb/image/upload/t_screenshot_huge/byull3k2xzfndgivkdlw.jpg",
        "http://images.igdb.com/igdb/image/upload/t_screenshot_huge/o0pqdynpsv7vvziaxwzr.jpg"
    )
}