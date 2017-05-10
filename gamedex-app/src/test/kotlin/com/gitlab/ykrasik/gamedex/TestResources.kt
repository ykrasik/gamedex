package com.gitlab.ykrasik.gamedex

import com.gitlab.ykrasik.gamedex.test.*

/**
 * User: ykrasik
 * Date: 01/04/2017
 * Time: 14:09
 */
fun randomMetaData() = MetaData(
    libraryId = 1,
    path = randomFile(),
    lastModified = randomDateTime()
)

fun randomGameData() = GameData(
    name = randomString(),
    description = randomSentence(maxWords = 10),
    releaseDate = randomLocalDate(),
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