/****************************************************************************
 * Copyright (C) 2016-2019 Yevgeny Krasik                                   *
 *                                                                          *
 * Licensed under the Apache License, Version 2.0 (the "License");          *
 * you may not use this file except in compliance with the License.         *
 * You may obtain a copy of the License at                                  *
 *                                                                          *
 * http://www.apache.org/licenses/LICENSE-2.0                               *
 *                                                                          *
 * Unless required by applicable law or agreed to in writing, software      *
 * distributed under the License is distributed on an "AS IS" BASIS,        *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. *
 * See the License for the specific language governing permissions and      *
 * limitations under the License.                                           *
 ****************************************************************************/

package com.gitlab.ykrasik.gamedex.provider.igdb

import com.gitlab.ykrasik.gamedex.GameData
import com.gitlab.ykrasik.gamedex.ImageUrls
import com.gitlab.ykrasik.gamedex.Platform
import com.gitlab.ykrasik.gamedex.Score
import com.gitlab.ykrasik.gamedex.provider.ProviderDownloadData
import com.gitlab.ykrasik.gamedex.provider.ProviderSearchResult
import com.gitlab.ykrasik.gamedex.test.ScopedWordSpec
import com.gitlab.ykrasik.gamedex.test.assertScore
import com.gitlab.ykrasik.gamedex.test.testHttpClient
import com.typesafe.config.ConfigFactory
import io.kotlintest.matchers.haveSize
import io.kotlintest.matchers.should
import io.kotlintest.matchers.shouldBe

/**
 * User: ykrasik
 * Date: 12/04/2017
 * Time: 10:03
 */
class IgdbProviderContractTest : ScopedWordSpec<IgdbProviderContractTest.Scope>() {
    val config = IgdbConfig(ConfigFactory.load("com/gitlab/ykrasik/gamedex/provider/igdb/igdb.conf"))
    val provider = IgdbProvider(config, IgdbClient(config, testHttpClient))
    val account = IgdbUserAccount(apiKey = System.getProperty("gameDex.igdb.apiKey"))

    override fun scope() = Scope()

    init {
        "IgdbProvider" should {
            "search & retrieve a single search result" test {
                val results = provider.search(name, Platform.pc, account)
                results should haveSize(1)

                val result = results.first()
                result shouldBe ProviderSearchResult(
                    apiUrl = apiUrl,
                    name = name,
                    description = description,
                    releaseDate = releaseDate,
                    criticScore = result.criticScore.verifiedCriticScore,
                    userScore = result.userScore.verifiedUserScore,
                    thumbnailUrl = thumbnailUrl
                )
            }

            "download game details" test {
                val result = provider.download(apiUrl, Platform.pc, account)
                result shouldBe ProviderDownloadData(
                    gameData = GameData(
                        name = name,
                        description = description,
                        releaseDate = releaseDate,
                        criticScore = result.gameData.criticScore.verifiedCriticScore,
                        userScore = result.gameData.userScore.verifiedUserScore,
                        genres = genres,
                        imageUrls = ImageUrls(
                            thumbnailUrl = thumbnailUrl,
                            posterUrl = posterUrl,
                            screenshotUrls = screenshotUrls
                        )
                    ),
                    siteUrl = url
                )
            }
        }
    }

    class Scope {
        val name = "No Man's Sky"
        val apiUrl = "https://api-endpoint.igdb.com/games/3225"
        val releaseDate = "2016-08-12"
        val Score?.verifiedCriticScore get() = assertScore(min = 72, max = 74, numReviews = 30)
        val Score?.verifiedUserScore get() = assertScore(min = 62, max = 66, numReviews = 80)
        val thumbnailUrl = "http://images.igdb.com/igdb/image/upload/t_thumb_2x/ppt92xozmp6fcaxfce0h.jpg"
        val posterUrl = "http://images.igdb.com/igdb/image/upload/t_screenshot_huge/ppt92xozmp6fcaxfce0h.jpg"
        val url = "https://www.igdb.com/games/no-man-s-sky"
        val description =
            "Inspired by the adventure and imagination that we love from classic science-fiction, No Man's Sky presents you with a galaxy to explore, filled with unique planets and lifeforms, and constant danger and action. \n \nIn No Man's Sky, every star is the light of a distant sun, each orbited by planets filled with life, and you can go to any of them you choose. Fly smoothly from deep space to planetary surfaces, with no loading screens, and no limits. In this infinite procedurally generated universe, you'll discover places and creatures that no other players have seen before - and perhaps never will again."
        val genres = listOf("Shooter", "Role-Playing Game (RPG)", "Simulation", "Adventure", "Indie")
        val screenshotUrls = listOf(
            "http://images.igdb.com/igdb/image/upload/t_screenshot_huge/mdhzsdazyj8vyht9wnnj.jpg",
            "http://images.igdb.com/igdb/image/upload/t_screenshot_huge/bynzojxiouy00pqw1utm.jpg",
            "http://images.igdb.com/igdb/image/upload/t_screenshot_huge/s6p3zqbfof7kncyp7ocf.jpg",
            "http://images.igdb.com/igdb/image/upload/t_screenshot_huge/wihfvjowvwt4lx0qw5eo.jpg",
            "http://images.igdb.com/igdb/image/upload/t_screenshot_huge/hlv3vnzamh1l3pdc2omn.jpg",
            "http://images.igdb.com/igdb/image/upload/t_screenshot_huge/slbfwlrvqjrgvrvfghrk.jpg",
            "http://images.igdb.com/igdb/image/upload/t_screenshot_huge/xufkotfn5udk2aijb6f0.jpg",
            "http://images.igdb.com/igdb/image/upload/t_screenshot_huge/kmyailnbyy0afbyqdfxn.jpg",
            "http://images.igdb.com/igdb/image/upload/t_screenshot_huge/byull3k2xzfndgivkdlw.jpg",
            "http://images.igdb.com/igdb/image/upload/t_screenshot_huge/o0pqdynpsv7vvziaxwzr.jpg"
        )
    }
}