/****************************************************************************
 * Copyright (C) 2016-2018 Yevgeny Krasik                                   *
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

import com.gitlab.ykrasik.gamedex.*
import com.gitlab.ykrasik.gamedex.provider.ProviderSearchResult
import com.gitlab.ykrasik.gamedex.test.ScopedWordSpec
import com.typesafe.config.ConfigFactory
import io.kotlintest.matchers.shouldBe

/**
 * User: ykrasik
 * Date: 08/10/2016
 * Time: 17:59
 */
class GiantBombProviderContractTest : ScopedWordSpec() {
    val config = GiantBombConfig(ConfigFactory.load("com/gitlab/ykrasik/gamedex/provider/giantbomb/giantbomb.conf"))
    val provider = GiantBombProvider(config, GiantBombClient(config))
    val account = GiantBombUserAccount(apiKey = System.getProperty("gameDex.giantBomb.apiKey"))

    init {
        "GiantBombProvider" should {
            "search & retrieve a single search result" {
                provider.search(name, Platform.pc, account) shouldBe listOf(
                    ProviderSearchResult(
                        apiUrl = apiUrl,
                        name = name,
                        releaseDate = releaseDate,
                        criticScore = null,
                        userScore = null,
                        thumbnailUrl = thumbnailUrl
                    )
                )
            }

            "download game details" {
                provider.download(apiUrl, Platform.pc, account) shouldBe ProviderData(
                    header = ProviderHeader(
                        id = "GiantBomb",
                        apiUrl = apiUrl,
                        timestamp = nowMock
                    ),
                    gameData = GameData(
                        siteUrl = url,
                        name = name,
                        description = deck,
                        releaseDate = releaseDate,
                        criticScore = null,
                        userScore = null,
                        genres = genres,
                        imageUrls = ImageUrls(
                            thumbnailUrl = thumbnailUrl,
                            posterUrl = posterUrl,
                            screenshotUrls = screenshotUrls
                        )
                    )
                )
            }
        }
    }

    val name = "No Man's Sky"
    val apiUrl = "https://www.giantbomb.com/api/game/3030-44656/"
    val releaseDate = "2016-08-09"
    val thumbnailUrl = "https://www.giantbomb.com/api/image/scale_avatar/2927125-no%20man%27s%20sky.jpg"
    val posterUrl = "https://www.giantbomb.com/api/image/scale_large/2927125-no%20man%27s%20sky.jpg"
    val url = "https://www.giantbomb.com/no-mans-sky/3030-44656/"
    val deck = "A procedurally generated space exploration game from Hello Games, the creators of Joe Danger."
    val genres = listOf("Simulation", "Action-Adventure")
    val screenshotUrls = listOf(
        "https://static.giantbomb.com/uploads/scale_large/1/14876/3016654-no+man%27s+sky_20180418134254.jpg",
        "https://static.giantbomb.com/uploads/scale_large/1/14876/3016653-no+man%27s+sky_20180417232953.jpg",
        "https://static.giantbomb.com/uploads/scale_large/1/14876/3016652-no+man%27s+sky_20180417231341.jpg",
        "https://static.giantbomb.com/uploads/scale_large/1/14876/3016651-no+man%27s+sky_20180417231002.jpg",
        "https://static.giantbomb.com/uploads/scale_large/1/14876/3016650-no+man%27s+sky_20180417230157.jpg",
        "https://static.giantbomb.com/uploads/scale_large/1/14876/3016649-no+man%27s+sky_20180417223414.jpg",
        "https://static.giantbomb.com/uploads/scale_large/1/14876/3016648-no+man%27s+sky_20180417211455.jpg",
        "https://static.giantbomb.com/uploads/scale_large/1/14876/3016647-no+man%27s+sky_20180412171335.jpg",
        "https://static.giantbomb.com/uploads/scale_large/1/14876/3016646-no+man%27s+sky_20180411235409.jpg",
        "https://static.giantbomb.com/uploads/scale_large/1/14876/3016645-no+man%27s+sky_20180411231532.jpg",
        "https://static.giantbomb.com/uploads/scale_large/1/14876/3016644-no+man%27s+sky_20180411163306.jpg",
        "https://static.giantbomb.com/uploads/scale_large/1/14876/3016643-no+man%27s+sky_20180410230119.jpg",
        "https://static.giantbomb.com/uploads/scale_large/1/14876/3016642-no+man%27s+sky_20180410224229.jpg",
        "https://static.giantbomb.com/uploads/scale_large/1/14876/3016641-no+man%27s+sky_20180403055153.jpg",
        "https://static.giantbomb.com/uploads/scale_large/1/14876/3016640-no+man%27s+sky_20180402152217.jpg",
        "https://static.giantbomb.com/uploads/scale_large/1/14876/3016639-no+man%27s+sky_20180402133357.jpg",
        "https://static.giantbomb.com/uploads/scale_large/1/14876/3016638-no+man%27s+sky_20180402124827.jpg",
        "https://static.giantbomb.com/uploads/scale_large/1/14876/3016637-no+man%27s+sky_20180402124317.jpg",
        "https://static.giantbomb.com/uploads/scale_large/1/14876/3016636-no+man%27s+sky_20180330135608.jpg",
        "https://static.giantbomb.com/uploads/scale_large/1/14876/3016635-no+man%27s+sky_20180330134318.jpg",
        "https://static.giantbomb.com/uploads/scale_large/1/14876/3016634-no+man%27s+sky_20180316091511.jpg",
        "https://static.giantbomb.com/uploads/scale_large/1/14876/3016633-no+man%27s+sky_20180316084537.jpg",
        "https://static.giantbomb.com/uploads/scale_large/1/14876/3016632-no+man%27s+sky_20180316053710.jpg",
        "https://static.giantbomb.com/uploads/scale_large/1/14876/3016631-no+man%27s+sky_20180315183830.jpg",
        "https://static.giantbomb.com/uploads/scale_large/1/14876/3016630-no+man%27s+sky_20180315143647.jpg",
        "https://static.giantbomb.com/uploads/scale_large/1/14876/3016629-no+man%27s+sky_20180315142209.jpg",
        "https://static.giantbomb.com/uploads/scale_large/1/14876/3016628-no+man%27s+sky_20180315131841.jpg",
        "https://static.giantbomb.com/uploads/scale_large/1/14876/3016627-no+man%27s+sky_20180315131525.jpg",
        "https://static.giantbomb.com/uploads/scale_large/1/14876/3016626-no+man%27s+sky_20180315130116.jpg",
        "https://static.giantbomb.com/uploads/scale_large/1/14876/3016625-no+man%27s+sky_20180315033803.jpg"
    )
}