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
    val giantBombServer = GiantBombFakeServer(9001)
    val igdbServer = IgdbFakeServer(9002)

    @JvmStatic fun main(args: Array<String>) {
        appConfig = appConfig
            .withValue("gameDex.persistence.dbUrl", ConfigValueFactory.fromAnyRef("jdbc:h2:./test"))
            .withValue("gameDex.provider.giantBomb.endpoint", ConfigValueFactory.fromAnyRef(giantBombServer.endpointUrl))
            .withValue("gameDex.provider.igdb.endpoint", ConfigValueFactory.fromAnyRef(igdbServer.endpointUrl))
            .withValue("gameDex.provider.igdb.baseImageUrl", ConfigValueFactory.fromAnyRef(igdbServer.baseImageUrl))

        giantBombServer.start()
        igdbServer.start()

//        generateDb()

        Main.main(args)

        igdbServer.close()
        giantBombServer.close()
    }

    private fun generateDb() {
        val numGames = 1000

        val guice = GuiceDiContainer(listOf(PersistenceModule, ConfigModule))
        guice.getInstance(DbInitializer::class).reload()
        
        val persistenceService = guice.getInstance(PersistenceService::class)

        persistenceService.insertLibrary(
            path = javaClass.getResource("/test-games").toURI().toFile(), data = LibraryData(Platform.pc, "Test Games")
        )

        val executor = Executors.newFixedThreadPool(10)
        val context = executor.asCoroutineDispatcher()
        runBlocking {
            (0..numGames-1).map {
                async(context) {
                    val providerTypes = mutableListOf(*GameProviderType.values())
                    persistenceService.insertGame(
                        metaData = randomMetaData(),
                        rawGameData = List(rnd.nextInt(GameProviderType.values().size + 1)) {
                            val type = providerTypes.randomElement()
                            providerTypes -= type
                            randomRawGameData(type)
                        }
                    )
                }
            }.forEach { it.await() }
        }
        executor.shutdownNow()
    }

    private fun randomRawGameData(type: GameProviderType) = RawGameData(
        providerData = randomProviderData(type),
        gameData = randomGameData(),
        imageUrls = imageUrls(type)
    )

    private fun randomProviderData(type: GameProviderType) = ProviderData(
        type = type,
        apiUrl = apiUrl(type),
        siteUrl = randomUrl()
    )

    private fun apiUrl(type: GameProviderType) = when (type) {
        GameProviderType.GiantBomb -> giantBombServer.apiDetailsUrl
        GameProviderType.Igdb -> "${igdbServer.endpointUrl}/${rnd.nextInt()}"
    }

    private fun imageUrls(type: GameProviderType) = ImageUrls(
        thumbnailUrl = randomThumbnailUrl(type),
        posterUrl = randomPosterUrl(type),
        screenshotUrls = List(rnd.nextInt(10)) { randomScreenshotUrl(type) }
    )

    private fun randomThumbnailUrl(type: GameProviderType) = when (type) {
        GameProviderType.GiantBomb -> "${giantBombServer.thumbnailUrl}/${randomString()}.jpg"
        GameProviderType.Igdb -> "${igdbServer.thumbnailUrl}/${randomString()}.png"
    }

    private fun randomPosterUrl(type: GameProviderType) = when (type) {
        GameProviderType.GiantBomb -> "${giantBombServer.superUrl}/${randomString()}.jpg"
        GameProviderType.Igdb -> "${igdbServer.posterUrl}/${randomString()}.png"
    }

    private fun randomScreenshotUrl(type: GameProviderType) = when (type) {
        GameProviderType.GiantBomb -> "${giantBombServer.screenshotUrl}/${randomString()}.jpg"
        GameProviderType.Igdb -> "${igdbServer.screenshotUrl}/${randomString()}.png"
    }
}