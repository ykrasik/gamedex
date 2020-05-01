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

package com.gitlab.ykrasik.gamedex.provider.giantbomb

import com.gitlab.ykrasik.gamedex.GameData
import com.gitlab.ykrasik.gamedex.Platform
import com.gitlab.ykrasik.gamedex.provider.GameProvider
import com.gitlab.ykrasik.gamedex.test.Spec
import com.typesafe.config.ConfigFactory
import io.kotlintest.matchers.shouldBe

/**
 * User: ykrasik
 * Date: 08/10/2016
 * Time: 17:59
 */
class GiantBombProviderContractTest : Spec<GiantBombProviderContractTest.Scope>() {
    val config = GiantBombConfig(ConfigFactory.load("com/gitlab/ykrasik/gamedex/provider/giantbomb/giantbomb.conf"))
    val provider = GiantBombProvider(config, GiantBombClient(config))
    val account = GiantBombUserAccount(apiKey = System.getProperty("gameDex.giantBomb.apiKey"))

    init {
        "GiantBombProvider" should {
            "search & retrieve a single search result" test {
                provider.search(name, Platform.Windows, account, offset = 0, limit = 10).results shouldBe listOf(
                    GameProvider.SearchResult(
                        providerGameId = apiUrl,
                        name = name,
                        description = deck,
                        releaseDate = releaseDate,
                        criticScore = null,
                        userScore = null,
                        thumbnailUrl = thumbnailUrl
                    )
                )
            }

            "fetch game details" test {
                provider.fetch(apiUrl, Platform.Windows, account) shouldBe GameProvider.FetchResponse(
                    gameData = GameData(
                        name = name,
                        description = deck,
                        releaseDate = releaseDate,
                        criticScore = null,
                        userScore = null,
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
        val apiUrl = "https://www.giantbomb.com/api/game/3030-44656/"
        val releaseDate = "2016-08-09"
        val thumbnailUrl = "https://giantbomb1.cbsistatic.com/uploads/scale_avatar/0/3699/2927125-no%20man%27s%20sky.jpg"
        val posterUrl = "https://giantbomb1.cbsistatic.com/uploads/scale_large/0/3699/2927125-no%20man%27s%20sky.jpg"
        val url = "https://www.giantbomb.com/no-mans-sky/3030-44656/"
        val deck = "A procedurally generated space exploration game from Hello Games, the creators of Joe Danger."
        val genres = listOf("Simulation", "Action-Adventure")
        val screenshotUrls = listOf(
            "https://giantbomb1.cbsistatic.com/uploads/scale_large/6/66643/3038806-aug-1%403.11.36.jpg",
            "https://giantbomb1.cbsistatic.com/uploads/scale_large/6/66643/3038805-aug-1%402.38.2.jpg",
            "https://giantbomb1.cbsistatic.com/uploads/scale_large/1/14876/3036612-no%20man%27s%20sky_20180412003928.jpg",
            "https://giantbomb1.cbsistatic.com/uploads/scale_large/1/14876/3036611-no%20man%27s%20sky_20180503060656.jpg",
            "https://giantbomb1.cbsistatic.com/uploads/scale_large/1/14876/3036610-no%20man%27s%20sky_20180507060546.jpg",
            "https://giantbomb1.cbsistatic.com/uploads/scale_large/1/14876/3036609-no%20man%27s%20sky_20180503054218.jpg",
            "https://giantbomb1.cbsistatic.com/uploads/scale_large/1/14876/3036608-no%20man%27s%20sky_20180503093659.jpg",
            "https://giantbomb1.cbsistatic.com/uploads/scale_large/1/14876/3036607-no%20man%27s%20sky_20180503055521.jpg",
            "https://giantbomb1.cbsistatic.com/uploads/scale_large/1/14876/3036606-no%20man%27s%20sky_20180502092642.jpg",
            "https://giantbomb1.cbsistatic.com/uploads/scale_large/1/14876/3036605-no%20man%27s%20sky_20180503051728.jpg",
            "https://giantbomb1.cbsistatic.com/uploads/scale_large/1/14876/3036604-no%20man%27s%20sky_20180503040539.jpg",
            "https://giantbomb1.cbsistatic.com/uploads/scale_large/1/14876/3036603-no%20man%27s%20sky_20180502075738.jpg",
            "https://giantbomb1.cbsistatic.com/uploads/scale_large/1/14876/3036602-no%20man%27s%20sky_20180502203736.jpg",
            "https://giantbomb1.cbsistatic.com/uploads/scale_large/1/14876/3036601-no%20man%27s%20sky_20180502083627.jpg",
            "https://giantbomb1.cbsistatic.com/uploads/scale_large/1/14876/3036600-no%20man%27s%20sky_20180427175532.jpg",
            "https://giantbomb1.cbsistatic.com/uploads/scale_large/1/14876/3036599-no%20man%27s%20sky_20180330134905.jpg",
            "https://giantbomb1.cbsistatic.com/uploads/scale_large/1/14876/3036598-no%20man%27s%20sky_20180427001150.jpg",
            "https://giantbomb1.cbsistatic.com/uploads/scale_large/1/14876/3036597-no%20man%27s%20sky_20180426184952.jpg",
            "https://giantbomb1.cbsistatic.com/uploads/scale_large/1/14876/3036596-no%20man%27s%20sky_20180426183529.jpg",
            "https://giantbomb1.cbsistatic.com/uploads/scale_large/1/14876/3036595-no%20man%27s%20sky_20180424051117.jpg",
            "https://giantbomb1.cbsistatic.com/uploads/scale_large/1/14876/3036594-no%20man%27s%20sky_20180424044747.jpg",
            "https://giantbomb1.cbsistatic.com/uploads/scale_large/1/14876/3036593-no%20man%27s%20sky_20180411172614.jpg",
            "https://giantbomb1.cbsistatic.com/uploads/scale_large/1/14876/3016654-no%20man%27s%20sky_20180418134254.jpg",
            "https://giantbomb1.cbsistatic.com/uploads/scale_large/1/14876/3016653-no%20man%27s%20sky_20180417232953.jpg",
            "https://giantbomb1.cbsistatic.com/uploads/scale_large/1/14876/3016652-no%20man%27s%20sky_20180417231341.jpg",
            "https://giantbomb1.cbsistatic.com/uploads/scale_large/1/14876/3016651-no%20man%27s%20sky_20180417231002.jpg",
            "https://giantbomb1.cbsistatic.com/uploads/scale_large/1/14876/3016650-no%20man%27s%20sky_20180417230157.jpg",
            "https://giantbomb1.cbsistatic.com/uploads/scale_large/1/14876/3016649-no%20man%27s%20sky_20180417223414.jpg",
            "https://giantbomb1.cbsistatic.com/uploads/scale_large/1/14876/3016648-no%20man%27s%20sky_20180417211455.jpg",
            "https://giantbomb1.cbsistatic.com/uploads/scale_large/1/14876/3016647-no%20man%27s%20sky_20180412171335.jpg"
        )
    }
}