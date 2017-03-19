package com.gitlab.ykrasik.gamedex.ui.view.fragment

import com.gitlab.ykrasik.gamedex.common.datamodel.*
import com.gitlab.ykrasik.gamedex.common.testkit.*
import com.gitlab.ykrasik.gamedex.persistence.AddGameRequest
import com.gitlab.ykrasik.gamedex.provider.ProviderSearchResult
import com.gitlab.ykrasik.gamedex.provider.SearchContext
import javafx.beans.property.SimpleObjectProperty

/**
 * User: ykrasik
 * Date: 18/03/2017
 * Time: 20:48
 */
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
    bytes = TestImages.randomImageBytes()
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