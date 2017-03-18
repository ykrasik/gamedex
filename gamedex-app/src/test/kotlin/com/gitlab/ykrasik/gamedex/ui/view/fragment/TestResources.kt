package com.gitlab.ykrasik.gamedex.ui.view.fragment

import com.gitlab.ykrasik.gamedex.common.datamodel.*
import com.gitlab.ykrasik.gamedex.common.util.*
import com.gitlab.ykrasik.gamedex.persistence.AddGameRequest
import com.gitlab.ykrasik.gamedex.provider.ProviderSearchResult
import javafx.scene.image.ImageView
import org.joda.time.LocalDate
import tornadofx.observable
import java.io.File

/**
 * User: ykrasik
 * Date: 18/03/2017
 * Time: 20:48
 */
object TestImages {
    private fun loadImage(name: String) = getResourceAsByteArray(name)

    val images = (0..13).map { i ->
        try {
            loadImage("game$i.jpg")
        } catch (e: Exception) {
            loadImage("game$i.png")
        }
    }

    fun randomImage(): ImageView = images.randomElement().toImageView()
}

val testSearchResults = listOf(
    ProviderSearchResultView(ProviderSearchResult(
        name = "Assassin's Creed",
        releaseDate = LocalDate.parse("2007-11-13"),
        score = 89.1,
        thumbnailUrl = "http://www.giantbomb.com/api/image/scale_avatar/2843355-ac.jpg",
        apiUrl = "http://www.giantbomb.com/api/game/3030-2950/"
    ), TestImages.randomImage()),
    ProviderSearchResultView(ProviderSearchResult(
        name = "Assassin's Creed II",
        releaseDate = LocalDate.parse("2009-11-17"),
        score = 86.7,
        thumbnailUrl = "http://www.giantbomb.com/api/image/scale_avatar/2392977-assassins_creed_ii_05_artwork.jpg",
        apiUrl = "http://www.giantbomb.com/api/game/3030-22928/"
    ), TestImages.randomImage()),
    ProviderSearchResultView(ProviderSearchResult(
        name = "Assassin's Creed: Brotherhood",
        releaseDate = LocalDate.parse("2010-11-16"),
        score = 84.3,
        thumbnailUrl = "http://www.giantbomb.com/api/image/scale_avatar/2843337-acbro.jpg",
        apiUrl = "http://www.giantbomb.com/api/game/3030-31001/"
    ), TestImages.randomImage())
).observable()

fun randomAddGameRequest() = AddGameRequest(
    metaData = randomMetaData(),
    gameData = randomGameData(),
    providerData = randomProviderData(),
    imageUrls = randomImageUrls()
)

private fun randomMetaData() = MetaData(
    libraryId = 1,
    path = File(randomSlashDelimitedString()),
    lastModified = randomDateTime()
)

private fun randomGameData() = GameData(
    name = randomString(),
    description = randomSentence(),
    releaseDate = randomLocalDate(),
    criticScore = rnd.nextDouble() * 10,
    userScore = rnd.nextDouble() * 10,
    genres = List(rnd.nextInt(4)) { randomString(length = 4, variance = 4) }
)

private fun randomProviderData(): List<ProviderData> = List(rnd.nextInt(DataProviderType.values().size)) {
    ProviderData(
        type = randomEnum(),
        apiUrl = randomSlashDelimitedString(),
        url = randomSlashDelimitedString()
    )
}

private fun randomImageUrls() = ImageUrls(
    thumbnailUrl = randomSlashDelimitedString(),
    posterUrl = null,
    screenshotUrls = emptyList()
)

fun randomGameImage(id: Int) = GameImage(
    id = id,
    url = randomSlashDelimitedString(),
    bytes = TestImages.images.randomElement()
)

private fun randomSlashDelimitedString(): String = randomSentence(maxWords = 5, avgWordLength = 5, delimiter = "/")