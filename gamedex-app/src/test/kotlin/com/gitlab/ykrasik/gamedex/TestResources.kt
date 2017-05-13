package com.gitlab.ykrasik.gamedex

import com.gitlab.ykrasik.gamedex.test.*

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

fun randomMetaData() = MetaData(
    libraryId = 1,
    path = randomFile(),
    lastModified = randomDateTime()
)

fun randomGameData() = GameData(
    name = randomString(),
    description = randomSentence(maxWords = 10),
    releaseDate = randomLocalDateString(),
    criticScore = randomScore(),
    userScore = randomScore(),
    genres = List(rnd.nextInt(4)) { randomString() }
)

fun randomProviderHeaders(): List<ProviderHeader> = List(rnd.nextInt(GameProviderType.values().size)) {
    ProviderHeader(
        type = randomEnum(),
        apiUrl = randomUrl(),
        siteUrl = randomUrl()
    )
}

fun randomImageUrls() = ImageUrls(
    thumbnailUrl = randomUrl(),
    posterUrl = randomUrl(),
    screenshotUrls = List(rnd.nextInt(10)) { randomUrl() }
)