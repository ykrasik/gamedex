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

package com.gitlab.ykrasik.gamedex.provider.opencritic

import com.gitlab.ykrasik.gamedex.GameData
import com.gitlab.ykrasik.gamedex.Platform
import com.gitlab.ykrasik.gamedex.Score
import com.gitlab.ykrasik.gamedex.provider.GameProvider
import com.gitlab.ykrasik.gamedex.test.Spec
import com.gitlab.ykrasik.gamedex.test.assertScore
import com.typesafe.config.ConfigFactory
import io.kotlintest.matchers.haveSize
import io.kotlintest.matchers.should
import io.kotlintest.matchers.shouldBe

/**
 * User: ykrasik
 * Date: 28/09/2019
 * Time: 22:15
 */
class OpenCriticProviderContractTest : Spec<OpenCriticProviderContractTest.Scope>() {
    val config = OpenCriticConfig(ConfigFactory.load("com/gitlab/ykrasik/gamedex/provider/opencritic/opencritic.conf"))
    val provider = OpenCriticProvider(config, OpenCriticClient(config))

    init {
        "OpenCriticProvider" should {
            "search & retrieve a single search result" test {
                val results = provider.search(name, Platform.Windows, GameProvider.Account.Null, offset = 0, limit = 10).results
                results should haveSize(10)

                val result = results.first()
                result shouldBe GameProvider.SearchResult(
                    providerGameId = providerGameId,
                    name = name,
                    description = description,
                    releaseDate = releaseDate,
                    criticScore = result.criticScore.verifiedCriticScore,
                    userScore = null,
                    thumbnailUrl = null
                )
            }

            "fetch game details" test {
                val result = provider.fetch(providerGameId, Platform.Windows, GameProvider.Account.Null)
                result shouldBe GameProvider.FetchResponse(
                    gameData = GameData(
                        name = name,
                        description = description,
                        releaseDate = releaseDate,
                        criticScore = result.gameData.criticScore.verifiedCriticScore,
                        userScore = null,
                        genres = genres,
                        thumbnailUrl = null,
                        posterUrl = null,
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
        val providerGameId = "2393"
        val releaseDate = "2016-08-12"
        val Score?.verifiedCriticScore get() = assertScore(min = 65, max = 75, numReviews = 135)
        val url = "https://opencritic.com/game/2393/no-man-s-sky"
        val description =
            "Inspired by the adventure and imagination that we love from classic science-fiction, No Man's Sky presents you with a galaxy to explore, filled with unique planets and lifeforms, and constant danger and action. \n\n" +
                "In No Man's Sky, every star is the light of a distant sun, each orbited by planets filled with life, and you can go to any of them you choose. Fly smoothly from deep space to planetary surfaces, with no loading screens, and no limits. In this infinite procedurally generated universe, you'll discover places and creatures that no other players have seen before - and perhaps never will again."
        val genres = listOf("Survival", "Adventure")
        val screenshotUrls = listOf(
            "http://c.opencritic.com/images/games/2393/lbedCKA9ZkFv0kD6EqII1tiYm3mcOp1B.jpg",
            "http://c.opencritic.com/images/games/2393/tl0NTSmTitve31yK0PlhIqLrNOxS75Eo.jpg",
            "http://c.opencritic.com/images/games/2393/SnMYwWJ4bFWpCfRNrLv28Notvl22LxQj.jpg"
        )
    }
}