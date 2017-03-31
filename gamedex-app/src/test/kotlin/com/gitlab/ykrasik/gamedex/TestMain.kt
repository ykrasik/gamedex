package com.gitlab.ykrasik.gamedex

import com.gitlab.ykrasik.gamedex.common.datamodel.*
import com.gitlab.ykrasik.gamedex.common.testkit.*
import com.gitlab.ykrasik.gamedex.common.util.ConfigProvider
import com.gitlab.ykrasik.gamedex.common.util.defaultConfig
import com.gitlab.ykrasik.gamedex.common.util.toFile
import com.gitlab.ykrasik.gamedex.persistence.AddGameRequest
import com.gitlab.ykrasik.gamedex.persistence.AddLibraryRequest
import com.gitlab.ykrasik.gamedex.persistence.DbInitializer
import com.gitlab.ykrasik.gamedex.persistence.PersistenceService
import com.gitlab.ykrasik.gamedex.provider.giantbomb.GiantBombEmbeddedServer
import com.gitlab.ykrasik.gamedex.provider.igdb.IgdbEmbeddedServer
import com.typesafe.config.ConfigValueFactory
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.newFixedThreadPoolContext
import kotlinx.coroutines.experimental.runBlocking

/**
 * User: ykrasik
 * Date: 18/03/2017
 * Time: 21:59
 */
object TestMain {
    val numGames = 1000
    val giantBombPort = 9001
    val igdbPort = 9002

    @JvmStatic fun main(args: Array<String>) {
        ConfigProvider.setConfig(defaultConfig
            .withValue("gameDex.persistence.dbUrl", ConfigValueFactory.fromAnyRef("jdbc:h2:./test"))
            .withValue("gameDex.provider.giantBomb.endpoint", ConfigValueFactory.fromAnyRef("http://localhost:$giantBombPort/"))
            .withValue("gameDex.provider.igdb.endpoint", ConfigValueFactory.fromAnyRef("http://localhost:$igdbPort/"))
            .withValue("gameDex.provider.igdb.baseImageUrl", ConfigValueFactory.fromAnyRef("http://localhost:$igdbPort/images"))
        )

        GiantBombEmbeddedServer(giantBombPort).start(isFakeProd = true)
        IgdbEmbeddedServer(igdbPort).start()

//        generateDb()

        Main.main(args)
    }

    private fun generateDb() {
        Main.MainDiContainer.getInstance(DbInitializer::class).reload()
        
        val persistenceService = Main.MainDiContainer.getInstance(PersistenceService::class)

        persistenceService.insert(AddLibraryRequest(
            path = javaClass.getResource("/test-games").toURI().toFile(), data = LibraryData(GamePlatform.pc, "Test Games")
        ))

        val context = newFixedThreadPoolContext(10, "db-generator")
        runBlocking {
            (0..numGames-1).map {
                async(context) {
                    val game = persistenceService.insert(randomAddGameRequest())
                    persistenceService.updateImage(randomGameImage(game.imageIds.thumbnailId!!))
                }
            }.forEach { it.await() }
        }
    }

    private fun randomAddGameRequest() = AddGameRequest(
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
        description = randomSentence(maxWords = 10),
        releaseDate = randomLocalDate(),
        criticScore = randomScore(),
        userScore = randomScore(),
        genres = List(rnd.nextInt(4)) { randomString() }
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

    private fun randomGameImage(id: Int) = GameImage(
        id = id,
        url = randomSlashDelimitedString(),
        bytes = TestImages.randomImageBytes()
    )
}