package com.gitlab.ykrasik.gamedex.provider.giantbomb

import com.gitlab.ykrasik.gamedex.common.datamodel.*
import com.gitlab.ykrasik.gamedex.common.testkit.ScopedWordSpec
import com.gitlab.ykrasik.gamedex.provider.ProviderFetchResult
import com.gitlab.ykrasik.gamedex.provider.ProviderSearchResult
import org.joda.time.LocalDate

/**
 * User: ykrasik
 * Date: 08/10/2016
 * Time: 17:59
 */
class GiantBombDataProviderRealIT : ScopedWordSpec() {
    val provider = GiantBombDataProvider(GiantBombConfig())

    class Scope {
        val name = "No Man's Sky"
        val apiUrl = "https://www.giantbomb.com/api/game/3030-44656/"
        val releaseDate = LocalDate.parse("2016-08-09")
        val thumbnailUrl = "https://www.giantbomb.com/api/image/scale_avatar/2927125-no%20man%27s%20sky.jpg"
        val posterUrl = "https://www.giantbomb.com/api/image/scale_large/2927125-no%20man%27s%20sky.jpg"
        val url = "https://www.giantbomb.com/no-mans-sky/3030-44656/"
        val description = "A procedurally generated space exploration game from Hello Games, the creators of Joe Danger."
        val genres = listOf("Simulation", "Action-Adventure")

        val searchResult = ProviderSearchResult(
            apiUrl = apiUrl,
            name = name,
            releaseDate = releaseDate,
            score = null,
            thumbnailUrl = thumbnailUrl
        )
    }

    init {
        "GiantBombDataProvider" should {
            "Search & retrieve a single search result".inScope(Scope()) {
                val response = provider.search(name, GamePlatform.pc)
                response shouldBe listOf(searchResult)
            }

            "Fetch game details".inScope(Scope()) {
                val response = provider.fetch(searchResult)
                response shouldBe ProviderFetchResult(
                    providerData = ProviderData(
                        type = DataProviderType.GiantBomb,
                        apiUrl = apiUrl,
                        url = url
                    ),
                    gameData = GameData(
                        name = name,
                        description = description,
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
}