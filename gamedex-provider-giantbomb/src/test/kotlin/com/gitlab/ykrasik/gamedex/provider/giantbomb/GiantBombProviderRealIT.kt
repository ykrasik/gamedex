package com.gitlab.ykrasik.gamedex.provider.giantbomb

import com.gitlab.ykrasik.gamedex.*
import com.gitlab.ykrasik.gamedex.test.ScopedWordSpec
import com.gitlab.ykrasik.gamedex.util.appConfig
import io.kotlintest.matchers.shouldBe
import org.joda.time.LocalDate

/**
 * User: ykrasik
 * Date: 08/10/2016
 * Time: 17:59
 */
class GiantBombProviderRealIT : ScopedWordSpec() {
    val provider = GiantBombProvider(GiantBombClient(GiantBombConfig(appConfig)))

    init {
        "GiantBombDataProvider" should {
            "search & retrieve a single search result".inScope(Scope()) {
                provider.search(name, GamePlatform.pc) shouldBe listOf(ProviderSearchResult(
                    apiUrl = apiUrl,
                    name = name,
                    releaseDate = releaseDate,
                    score = null,
                    thumbnailUrl = thumbnailUrl
                ))
            }

            "fetch game details".inScope(Scope()) {
                provider.fetch(apiUrl, GamePlatform.pc) shouldBe RawGameData(
                    providerData = ProviderData(
                        type = GameProviderType.GiantBomb,
                        apiUrl = apiUrl,
                        siteUrl = url
                    ),
                    gameData = GameData(
                        name = name,
                        description = deck,
                        releaseDate = releaseDate,
                        criticScore = null,
                        userScore = null,
                        genres = genres
                    ),
                    imageUrls = ImageUrls(
                        thumbnailUrl = thumbnailUrl,
                        posterUrl = posterUrl,
                        screenshotUrls = emptyList()
                    )
                )
            }
        }
    }

    class Scope {
        val name = "No Man's Sky"
        val apiUrl = "https://www.giantbomb.com/api/game/3030-44656/"
        val releaseDate = LocalDate.parse("2016-08-09")
        val thumbnailUrl = "https://www.giantbomb.com/api/image/scale_avatar/2927125-no%20man%27s%20sky.jpg"
        val posterUrl = "https://www.giantbomb.com/api/image/scale_large/2927125-no%20man%27s%20sky.jpg"
        val url = "https://www.giantbomb.com/no-mans-sky/3030-44656/"
        val deck = "A procedurally generated space exploration game from Hello Games, the creators of Joe Danger."
        val genres = listOf("Simulation", "Action-Adventure")
    }
}