/****************************************************************************
 * Copyright (C) 2016-2020 Yevgeny Krasik                                   *
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
import com.gitlab.ykrasik.gamedex.Platform
import com.gitlab.ykrasik.gamedex.Score
import com.gitlab.ykrasik.gamedex.provider.GameProvider
import com.gitlab.ykrasik.gamedex.test.MockSingleValueStorage
import com.gitlab.ykrasik.gamedex.test.Spec
import com.gitlab.ykrasik.gamedex.test.assertScore
import com.typesafe.config.ConfigFactory
import io.kotest.matchers.collections.haveSize
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe

/**
 * User: ykrasik
 * Date: 12/04/2017
 * Time: 10:03
 */
class IgdbProviderContractTest : Spec<IgdbProviderContractTest.Scope>() {
    val config = IgdbConfig(ConfigFactory.load("com/gitlab/ykrasik/gamedex/provider/igdb/igdb.conf"))
    val provider = IgdbProvider(config, IgdbClient(config, MockSingleValueStorage()))
    val account = IgdbUserAccount(
        clientId = System.getProperty("gameDex.igdb.clientId"),
        clientSecret = System.getProperty("gameDex.igdb.clientSecret")
    )

    init {
        describe("IgdbProvider") {
            itShould("search & retrieve a single search result") {
                val results = provider.search(name, Platform.Windows, account, offset = 0, limit = 10).results
                results should haveSize(10)

                val result = results.first()
                result shouldBe GameProvider.SearchResult(
                    providerGameId = providerGameId,
                    name = name,
                    description = description,
                    releaseDate = releaseDate,
                    criticScore = result.criticScore.verifiedCriticScore,
                    userScore = result.userScore.verifiedUserScore,
                    thumbnailUrl = thumbnailUrl
                )
            }

            itShould("fetch game details") {
                val result = provider.fetch(providerGameId, Platform.Windows, account)
                result shouldBe GameProvider.FetchResponse(
                    gameData = GameData(
                        name = name,
                        description = description,
                        releaseDate = releaseDate,
                        criticScore = result.gameData.criticScore.verifiedCriticScore,
                        userScore = result.gameData.userScore.verifiedUserScore,
                        genres = genres,
                        thumbnailUrl = thumbnailUrl,
                        posterUrl = posterUrl,
                        screenshotUrls = screenshotUrls
                    ),
                    siteUrl = url
                )
            }
        }
    }

    override fun scope() = Scope()
    class Scope {
        val name = "No Man's Sky"
        val providerGameId = "3225"
        val releaseDate = "2016-08-12"
        val Score?.verifiedCriticScore get() = assertScore(min = 72, max = 74, numReviews = 43)
        val Score?.verifiedUserScore get() = assertScore(min = 65, max = 75, numReviews = 169)
        val thumbnailUrl = "http://images.igdb.com/igdb/image/upload/t_thumb_2x/co5j66.jpg"
        val posterUrl = "http://images.igdb.com/igdb/image/upload/t_screenshot_huge/co5j66.jpg"
        val url = "https://www.igdb.com/games/no-man-s-sky"
        val description =
            "Inspired by the adventure and imagination that we love from classic science-fiction, No Man's Sky presents you with a galaxy to explore, filled with unique planets and lifeforms, and constant danger and action.\n" +
                    "\n" +
                    "In No Man's Sky, every star is the light of a distant sun, each orbited by planets filled with life, and you can go to any of them you choose. Fly smoothly from deep space to planetary surfaces, with no loading screens, and no limits. In this infinite procedurally generated universe, you'll discover places and creatures that no other players have seen before - and perhaps never will again."
        val genres = listOf("Role-Playing Game (RPG)", "Simulation", "Adventure", "Indie")
        val screenshotUrls = listOf(
            "http://images.igdb.com/igdb/image/upload/t_screenshot_huge/slbfwlrvqjrgvrvfghrk.jpg",
            "http://images.igdb.com/igdb/image/upload/t_screenshot_huge/xufkotfn5udk2aijb6f0.jpg",
            "http://images.igdb.com/igdb/image/upload/t_screenshot_huge/kmyailnbyy0afbyqdfxn.jpg",
            "http://images.igdb.com/igdb/image/upload/t_screenshot_huge/byull3k2xzfndgivkdlw.jpg",
            "http://images.igdb.com/igdb/image/upload/t_screenshot_huge/o0pqdynpsv7vvziaxwzr.jpg",
            "http://images.igdb.com/igdb/image/upload/t_screenshot_huge/hlv3vnzamh1l3pdc2omn.jpg",
            "http://images.igdb.com/igdb/image/upload/t_screenshot_huge/wihfvjowvwt4lx0qw5eo.jpg",
            "http://images.igdb.com/igdb/image/upload/t_screenshot_huge/s6p3zqbfof7kncyp7ocf.jpg",
            "http://images.igdb.com/igdb/image/upload/t_screenshot_huge/bynzojxiouy00pqw1utm.jpg",
            "http://images.igdb.com/igdb/image/upload/t_screenshot_huge/mdhzsdazyj8vyht9wnnj.jpg"
        )
    }
}
