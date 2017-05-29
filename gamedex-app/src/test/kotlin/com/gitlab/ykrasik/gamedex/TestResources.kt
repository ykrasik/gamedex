package com.gitlab.ykrasik.gamedex

import com.gitlab.ykrasik.gamedex.test.*
import com.gitlab.ykrasik.gamedex.util.now

/**
 * User: ykrasik
 * Date: 01/04/2017
 * Time: 14:09
 */
fun randomLibrary() = Library(
    id = rnd.nextInt(),
    path = randomFile(),
    data = LibraryData(
        platform = randomEnum(),
        name = randomName()
    )
)

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

fun randomProviderHeaders(): List<ProviderHeader> = List(rnd.nextInt(GameProviderType.values().size)) {
    randomProviderHeader()
}

fun randomProviderHeader(type: GameProviderType = randomEnum(),
                         apiUrl: String = randomUrl()) = ProviderHeader(
    type = type,
    apiUrl = apiUrl,
    updateDate = now.minusYears(1)
)

fun randomImageUrls() = ImageUrls(
    thumbnailUrl = randomUrl(),
    posterUrl = randomUrl(),
    screenshotUrls = List(rnd.nextInt(10)) { randomUrl() }
)