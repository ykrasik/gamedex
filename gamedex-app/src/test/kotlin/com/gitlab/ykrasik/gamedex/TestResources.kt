package com.gitlab.ykrasik.gamedex

import com.gitlab.ykrasik.gamedex.common.testkit.*
import com.gitlab.ykrasik.gamedex.datamodel.DataProviderType
import com.gitlab.ykrasik.gamedex.datamodel.GameData
import com.gitlab.ykrasik.gamedex.datamodel.MetaData
import com.gitlab.ykrasik.gamedex.datamodel.ProviderData

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

fun randomProviderData(): List<ProviderData> = List(rnd.nextInt(DataProviderType.values().size)) {
    ProviderData(
        type = randomEnum(),
        apiUrl = randomUrl(),
        url = randomUrl()
    )
}