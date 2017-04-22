package com.gitlab.ykrasik.gamedex

import com.gitlab.ykrasik.gamedex.module.ConfigModule
import com.gitlab.ykrasik.gamedex.module.GuiceDiContainer
import com.gitlab.ykrasik.gamedex.persistence.DbInitializer
import com.gitlab.ykrasik.gamedex.persistence.PersistenceService
import com.gitlab.ykrasik.gamedex.persistence.module.PersistenceModule
import com.gitlab.ykrasik.gamedex.provider.giantbomb.GiantBombFakeServer
import com.gitlab.ykrasik.gamedex.provider.igdb.IgdbFakeServer
import com.gitlab.ykrasik.gamedex.test.randomElement
import com.gitlab.ykrasik.gamedex.test.randomString
import com.gitlab.ykrasik.gamedex.test.randomUrl
import com.gitlab.ykrasik.gamedex.test.rnd
import com.gitlab.ykrasik.gamedex.util.appConfig
import com.gitlab.ykrasik.gamedex.util.toFile
import com.typesafe.config.ConfigValueFactory
import kotlinx.coroutines.experimental.asCoroutineDispatcher
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.runBlocking
import java.util.concurrent.Executors

/**
 * User: ykrasik
 * Date: 18/03/2017
 * Time: 21:59
 */
object TestMain {
    val numGames = 1000
    val giantBombPort = 9001
    val igdbPort = 9002

    val giantBombUrl = "http://localhost:$giantBombPort/"
    val giantBombImageUrl = "${giantBombUrl}images"

    val igdbUrl = "http://localhost:$igdbPort/"
    val igdbImageUrl = "${igdbUrl}images"

    @JvmStatic fun main(args: Array<String>) {
        appConfig = appConfig
            .withValue("gameDex.persistence.dbUrl", ConfigValueFactory.fromAnyRef("jdbc:h2:./test"))
            .withValue("gameDex.provider.giantBomb.endpoint", ConfigValueFactory.fromAnyRef(giantBombUrl))
            .withValue("gameDex.provider.igdb.endpoint", ConfigValueFactory.fromAnyRef(igdbUrl))
            .withValue("gameDex.provider.igdb.baseImageUrl", ConfigValueFactory.fromAnyRef(igdbImageUrl))

        val giantBombServer = GiantBombFakeServer(giantBombPort).start()
        val igdbServer = IgdbFakeServer(igdbPort).start()

//        generateDb()

        Main.main(args)
        igdbServer.close()
        giantBombServer.close()
    }

    private fun generateDb() {
        val guice = GuiceDiContainer(listOf(PersistenceModule, ConfigModule))
        guice.getInstance(DbInitializer::class).reload()
        
        val persistenceService = guice.getInstance(PersistenceService::class)

        persistenceService.insertLibrary(
            path = javaClass.getResource("/test-games").toURI().toFile(), data = LibraryData(GamePlatform.pc, "Test Games")
        )

        val executor = Executors.newFixedThreadPool(10)
        val context = executor.asCoroutineDispatcher()
        runBlocking {
            (0..numGames-1).map {
                async(context) {
                    val providerTypes = mutableListOf(*DataProviderType.values())
                    persistenceService.insertGame(
                        metaData = randomMetaData(),
                        providerData = List(rnd.nextInt(DataProviderType.values().size + 1)) {
                            val type = providerTypes.randomElement()
                            providerTypes -= type
                            randomProviderFetchResult(type)
                        }
                    )
                }
            }.forEach { it.await() }
        }
        executor.shutdownNow()
    }

    private fun randomProviderFetchResult(type: DataProviderType) = ProviderFetchResult(
        providerData = randomProviderData(type),
        gameData = randomGameData(),
        imageUrls = imageUrls(type)
    )

    private fun imageUrls(type: DataProviderType) = ImageUrls(
        thumbnailUrl = imageUrl(type),
        posterUrl = imageUrl(type),
        screenshotUrls = List(rnd.nextInt(10)) { imageUrl(type) }
    )

    private fun imageUrl(type: DataProviderType) = when (type) {
        DataProviderType.GiantBomb -> "$giantBombImageUrl/${randomString()}"
        DataProviderType.Igdb -> "$igdbImageUrl/t_thumb_2x/${randomString()}.png"
    }

    private fun randomProviderData(type: DataProviderType) = ProviderData(
        type = type,
        apiUrl = when(type) {
            DataProviderType.GiantBomb -> giantBombUrl
            DataProviderType.Igdb -> igdbUrl
        },
        siteUrl = randomUrl()
    )
}