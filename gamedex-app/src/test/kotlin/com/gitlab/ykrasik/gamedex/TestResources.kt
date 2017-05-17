package com.gitlab.ykrasik.gamedex

import com.gitlab.ykrasik.gamedex.test.*
import org.joda.time.DateTime

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
    lastModified = DateTime.now()
)

fun randomGameData() = GameData(
    name = randomString(),
    description = randomSentence(maxWords = 10),
    releaseDate = randomLocalDateString(),
    criticScore = randomScore(),
    userScore = randomScore(),
    genres = List(rnd.nextInt(5)) { "Genre ${rnd.nextInt(30)}" }
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