package com.gitlab.ykrasik.gamedex.ui.view.fragment

import com.gitlab.ykrasik.gamedex.common.datamodel.*
import com.gitlab.ykrasik.gamedex.common.util.getResourceAsByteArray
import com.gitlab.ykrasik.gamedex.common.util.toImage
import com.gitlab.ykrasik.gamedex.persistence.AddGameRequest
import com.gitlab.ykrasik.gamedex.provider.ProviderSearchResult
import com.gitlab.ykrasik.gamedex.provider.SearchContext
import javafx.beans.property.SimpleObjectProperty
import javafx.scene.image.Image
import org.joda.time.DateTime
import org.joda.time.LocalDate
import java.io.File
import java.util.*

/**
 * User: ykrasik
 * Date: 18/03/2017
 * Time: 20:48
 */
private val rnd = Random()

object TestImages {
    private fun loadImage(name: String) = getResourceAsByteArray(name)

    val images = (0..13).map { i ->
        try {
            loadImage("game$i.jpg")
        } catch (e: Exception) {
            loadImage("game$i.png")
        }
    }

    fun randomImage(): Image = images.randomElement().toImage()
}

fun randomAddGameRequest() = AddGameRequest(
    metaData = randomMetaData(),
    gameData = randomGameData(),
    providerData = randomProviderData(),
    imageUrls = randomImageUrls()
)

private fun randomMetaData() = MetaData(
    libraryId = 1,
    path = randomFile(),
    lastModified = randomDateTime()
)

private fun randomGameData() = GameData(
    name = randomString(),
    description = randomSentence(),
    releaseDate = randomLocalDate(),
    criticScore = randomScore(),
    userScore = randomScore(),
    genres = List(rnd.nextInt(4)) { randomString(length = 4, variance = 4) }
)

private fun randomProviderData(): List<ProviderData> = List(rnd.nextInt(DataProviderType.values().size)) {
    ProviderData(
        type = randomEnum(),
        apiUrl = randomUrl(),
        url = randomUrl()
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

fun randomSearchContext() = SearchContext(
    searchedName = randomString(10, variance = 3),
    path = randomFile()
)

fun randomSearchResults(amount: Int) = List(amount) {
    ProviderSearchResultView(ProviderSearchResult(
        name = randomSentence(maxWords = 4, avgWordLength = 10, variance = 2),
        releaseDate = randomLocalDate(),
        score = randomScore(),
        thumbnailUrl = randomUrl(),
        apiUrl = randomUrl()
    ), SimpleObjectProperty(TestImages.randomImage()))
}

private fun randomSlashDelimitedString(): String = randomSentence(maxWords = 7, avgWordLength = 5, delimiter = "/")
private fun randomFile() = File(randomSlashDelimitedString())
private fun randomUrl() = "http://${randomSentence(maxWords = 3, avgWordLength = 5, variance = 2, delimiter = ".")}/${randomSlashDelimitedString()}/${randomString(length = 5, variance = 3)}"
private fun randomScore() = rnd.nextDouble() * 100

private val chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
private fun randomString(length: Int = 20, variance: Int = 0): String {
    val str = StringBuilder()
    repeat(length.withVariance(variance)) {
        str.append(chars[com.gitlab.ykrasik.gamedex.common.util.rnd.nextInt(chars.length)])
    }
    return str.toString()
}

private fun randomSentence(maxWords: Int = 10, avgWordLength: Int = 20, variance: Int = 5, delimiter: String = " "): String {
    val str = StringBuilder()
    repeat(maxOf(com.gitlab.ykrasik.gamedex.common.util.rnd.nextInt(maxWords), 3)) {
        str.append(randomString(avgWordLength, variance))
        str.append(delimiter)
    }
    return str.toString()
}

private fun Int.withVariance(variance: Int): Int =
    if (variance == 0) {
        this
    } else {
        val multiplier: Int = com.gitlab.ykrasik.gamedex.common.util.rnd.nextInt(variance) + 1
        if (com.gitlab.ykrasik.gamedex.common.util.rnd.nextBoolean()) (this * multiplier) else (this / multiplier)
    }

private fun randomDateTime(): DateTime = DateTime(com.gitlab.ykrasik.gamedex.common.util.rnd.nextLong())
private fun randomLocalDate(): LocalDate = randomDateTime().toLocalDate()

private inline fun <reified E : Enum<E>> randomEnum(): E = E::class.java.enumConstants.randomElement()

private fun <T> List<T>.randomElement(): T = this[com.gitlab.ykrasik.gamedex.common.util.rnd.nextInt(size)]
private fun <T> Array<T>.randomElement(): T = this[com.gitlab.ykrasik.gamedex.common.util.rnd.nextInt(size)]