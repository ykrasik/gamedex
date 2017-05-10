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
            "search & retrieve a single search result" {
                provider.search(name, Platform.pc) shouldBe listOf(ProviderSearchResult(
                    apiUrl = apiUrl,
                    name = name,
                    releaseDate = releaseDate,
                    score = null,
                    thumbnailUrl = thumbnailUrl
                ))
            }

            "fetch game details" {
                provider.fetch(apiUrl, Platform.pc) shouldBe ProviderData(
                    header = ProviderHeader(
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
                        screenshotUrls = screenshotUrls
                    )
                )
            }
        }
    }

    val name = "No Man's Sky"
    val apiUrl = "https://www.giantbomb.com/api/game/3030-44656/"
    val releaseDate = LocalDate.parse("2016-08-09")
    val thumbnailUrl = "https://www.giantbomb.com/api/image/scale_avatar/2927125-no%20man%27s%20sky.jpg"
    val posterUrl = "https://www.giantbomb.com/api/image/scale_large/2927125-no%20man%27s%20sky.jpg"
    val url = "https://www.giantbomb.com/no-mans-sky/3030-44656/"
    val deck = "A procedurally generated space exploration game from Hello Games, the creators of Joe Danger."
    val genres = listOf("Simulation", "Action-Adventure")
    val screenshotUrls = listOf(
        "https://static.giantbomb.com/uploads/scale_large/0/3699/2927125-no+man%27s+sky.jpg",
        "https://static.giantbomb.com/uploads/scale_large/0/5684/2888664-20160918132648_1.jpg",
        "https://static.giantbomb.com/uploads/scale_large/0/5684/2888663-20160918132159_1.jpg",
        "https://static.giantbomb.com/uploads/scale_large/0/5684/2888662-20160918131545_1.jpg",
        "https://static.giantbomb.com/uploads/scale_large/0/5684/2888661-20160918131538_1.jpg",
        "https://static.giantbomb.com/uploads/scale_large/0/5684/2888660-20160918130507_1.jpg",
        "https://static.giantbomb.com/uploads/scale_large/0/5684/2888659-20160918125007_1.jpg",
        "https://static.giantbomb.com/uploads/scale_large/0/5684/2888658-20160918124920_1.jpg",
        "https://static.giantbomb.com/uploads/scale_large/0/5684/2888657-20160918124730_1.jpg",
        "https://static.giantbomb.com/uploads/scale_large/0/5684/2888656-20160918124710_1.jpg",
        "https://static.giantbomb.com/uploads/scale_large/0/5684/2888655-20160918123455_1.jpg",
        "https://static.giantbomb.com/uploads/scale_large/0/5684/2888654-20160918122956_1.jpg",
        "https://static.giantbomb.com/uploads/scale_large/0/5684/2888653-20160918121440_1.jpg",
        "https://static.giantbomb.com/uploads/scale_large/0/5684/2888652-20160918120459_1.jpg",
        "https://static.giantbomb.com/uploads/scale_large/0/5684/2888651-20160918120359_1.jpg",
        "https://static.giantbomb.com/uploads/scale_large/0/5684/2888650-20160918120229_1.jpg",
        "https://static.giantbomb.com/uploads/scale_large/0/5684/2888649-20160918120220_1.jpg",
        "https://static.giantbomb.com/uploads/scale_large/0/5684/2888648-20160918120210_1.jpg",
        "https://static.giantbomb.com/uploads/scale_large/0/5684/2888647-20160918120147_1.jpg",
        "https://static.giantbomb.com/uploads/scale_large/0/5684/2888646-20160918120022_1.jpg",
        "https://static.giantbomb.com/uploads/scale_large/0/5684/2888645-20160918120015_1.jpg",
        "https://static.giantbomb.com/uploads/scale_large/0/5684/2888644-20160918115459_1.jpg",
        "https://static.giantbomb.com/uploads/scale_large/0/5684/2888643-20160918115418_1.jpg",
        "https://static.giantbomb.com/uploads/scale_large/0/5684/2888642-20160918115408_1.jpg",
        "https://static.giantbomb.com/uploads/scale_large/0/5684/2888641-20160918115344_1.jpg",
        "https://static.giantbomb.com/uploads/scale_large/0/5684/2888640-20160918115337_1.jpg",
        "https://static.giantbomb.com/uploads/scale_large/0/5684/2888639-20160918115328_1.jpg",
        "https://static.giantbomb.com/uploads/scale_large/0/5684/2888638-20160918115100_1.jpg",
        "https://static.giantbomb.com/uploads/scale_large/0/5684/2888637-20160918114757_1.jpg",
        "https://static.giantbomb.com/uploads/scale_large/0/5684/2888636-20160918114629_1.jpg"
    )
}