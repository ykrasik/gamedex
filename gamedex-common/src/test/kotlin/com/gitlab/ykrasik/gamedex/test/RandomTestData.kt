package com.gitlab.ykrasik.gamedex.test

import com.gitlab.ykrasik.gamedex.*
import com.gitlab.ykrasik.gamedex.util.now

/**
 * User: ykrasik
 * Date: 18/06/2017
 * Time: 11:24
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
fun Library.withPlatform(platform: Platform) = copy(data = data.copy(platform = platform))

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
            metadata = randomMetadata(),
            providerData = providerData,
            userData = null
        ),
        library = randomLibrary(),
        gameData = providerData.randomElement().gameData,
        providerHeaders = randomProviderHeaders(),
        imageUrls = randomImageUrls(),
        folderMetadata = randomFolderMetadata()
    )
}

fun Game.withPlatform(platform: Platform) = copy(library = library.withPlatform(platform))
fun Game.withCriticScore(score: Double?) = copy(gameData = gameData.withCriticScore(score))
fun Game.withUserScore(score: Double?) = copy(gameData = gameData.withUserScore(score))

fun randomMetadata(libraryId: Int = 1) = Metadata(
    libraryId = libraryId,
    path = randomPath(),
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

fun GameData.withCriticScore(score: Double?) = copy(criticScore = score?.let { randomScore().copy(score = it, numReviews = 99) })
fun GameData.withUserScore(score: Double?) = copy(userScore = score?.let { randomScore().copy(score = it, numReviews = 99) })

fun randomScore() = Score(score = rnd.nextDouble() * 100, numReviews = rnd.nextInt(30) + 1)

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

fun randomFolderMetadata() = FolderMetadata(
    rawName = randomString(),
    gameName = randomString(),
    order = null,
    metaTag = null,
    version = null
)