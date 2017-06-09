package com.gitlab.ykrasik.gamedex

import com.gitlab.ykrasik.gamedex.test.*
import com.gitlab.ykrasik.gamedex.util.now

/**
 * User: ykrasik
 * Date: 01/04/2017
 * Time: 14:09
 */
val testProviderIds = listOf("Igdb", "GiantBomb")

fun randomLibrary() = Library(
    id = rnd.nextInt(),
    path = randomFile(),
    data = LibraryData(
        platform = randomEnum(),
        name = randomName()
    )
)

fun randomGame(): Game {
    val providerData = testProviderIds.map { id ->
        ProviderData(
            header = randomProviderHeader(id),
            gameData = randomGameData(),
            imageUrls = randomImageUrls()
        )
    }
    return Game(
        rawGame = RawGame(
            id = rnd.nextInt(),
            metaData = randomMetaData(),
            providerData = providerData,
            userData = null
        ),
        library = randomLibrary(),
        gameData = providerData.randomElement().gameData,
        providerHeaders = randomProviderHeaders(),
        imageUrls = randomImageUrls()
    )
}

fun randomMetaData(libraryId: Int = 1) = MetaData(
    libraryId = libraryId,
    path = randomFile(),
    updateDate = now
)

fun randomGameData() = GameData(
    siteUrl = randomUrl(),
    name = randomString(),
    description = randomSentence(maxWords = 10),
    releaseDate = randomLocalDateString(),
    criticScore = randomScore(),
    userScore = randomScore(),
    genres = List(rnd.nextInt(5)) { "Genre ${rnd.nextInt(30)}" }
)

fun randomProviderHeaders(): List<ProviderHeader> = List(rnd.nextInt(testProviderIds.size)) {
    randomProviderHeader()
}

fun randomProviderHeader(id: ProviderId = testProviderIds.randomElement(),
                         apiUrl: String = randomUrl()) = ProviderHeader(
    id = id,
    apiUrl = apiUrl,
    updateDate = now.minusYears(1)
)

fun randomImageUrls() = ImageUrls(
    thumbnailUrl = randomUrl(),
    posterUrl = randomUrl(),
    screenshotUrls = List(rnd.nextInt(10)) { randomUrl() }
)