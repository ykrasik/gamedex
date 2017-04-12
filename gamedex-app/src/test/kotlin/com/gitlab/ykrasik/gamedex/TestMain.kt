package com.gitlab.ykrasik.gamedex

import com.gitlab.ykrasik.gamedex.common.testkit.TestImages
import com.gitlab.ykrasik.gamedex.common.testkit.randomSlashDelimitedString
import com.gitlab.ykrasik.gamedex.common.util.ConfigProvider
import com.gitlab.ykrasik.gamedex.common.util.defaultConfig
import com.gitlab.ykrasik.gamedex.common.util.toFile
import com.gitlab.ykrasik.gamedex.datamodel.GameImage
import com.gitlab.ykrasik.gamedex.datamodel.GamePlatform
import com.gitlab.ykrasik.gamedex.datamodel.ImageUrls
import com.gitlab.ykrasik.gamedex.datamodel.LibraryData
import com.gitlab.ykrasik.gamedex.module.GuiceDiContainer
import com.gitlab.ykrasik.gamedex.persistence.AddGameRequest
import com.gitlab.ykrasik.gamedex.persistence.AddLibraryRequest
import com.gitlab.ykrasik.gamedex.persistence.DbInitializer
import com.gitlab.ykrasik.gamedex.persistence.PersistenceService
import com.gitlab.ykrasik.gamedex.persistence.module.PersistenceModule
import com.gitlab.ykrasik.gamedex.provider.giantbomb.GiantBombEmbeddedServer
import com.gitlab.ykrasik.gamedex.provider.igdb.IgdbFakeServer
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

        val giantBombServer = GiantBombEmbeddedServer(giantBombPort).start(isFakeProd = true)
        val igdbServer = IgdbFakeServer(igdbPort).start()

//        generateDb()

        Main.main(args)
        igdbServer.close()
        giantBombServer.close()
    }

    private fun generateDb() {
        val diContainer = GuiceDiContainer(listOf(PersistenceModule()))
        diContainer.getInstance(DbInitializer::class).reload()
        
        val persistenceService = diContainer.getInstance(PersistenceService::class)

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

    private fun randomGameImage(id: Int) = GameImage(
        id = id,
        url = randomSlashDelimitedString(),
        bytes = TestImages.randomImageBytes()
    )

    private fun randomImageUrls() = ImageUrls(
        thumbnailUrl = randomSlashDelimitedString(),
        posterUrl = null,
        screenshotUrls = emptyList()
    )
}