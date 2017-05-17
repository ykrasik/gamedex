package com.gitlab.ykrasik.gamedex

import com.gitlab.ykrasik.gamedex.core.newDirectoryDetector
import com.gitlab.ykrasik.gamedex.module.ConfigModule
import com.gitlab.ykrasik.gamedex.module.GuiceDiContainer
import com.gitlab.ykrasik.gamedex.persistence.PersistenceService
import com.gitlab.ykrasik.gamedex.persistence.module.PersistenceModule
import com.gitlab.ykrasik.gamedex.provider.giantbomb.GiantBombFakeServer
import com.gitlab.ykrasik.gamedex.provider.igdb.IgdbFakeServer
import com.gitlab.ykrasik.gamedex.test.*
import com.gitlab.ykrasik.gamedex.util.appConfig
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doAnswer
import com.nhaarman.mockito_kotlin.mock
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

        newDirectoryDetector = mock {
            on { detectNewDirectories(any(), any()) } doAnswer { List(rnd.nextInt(4)) { randomFile() } }
        }

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
        val persistenceService = guice.getInstance(PersistenceService::class)
        persistenceService.dropDb()

        val libraries = (1..5).zip(listOf(Platform.pc, Platform.android, Platform.mac, Platform.excluded, Platform.pc)).map { (i, platform) ->
            val name = "lib$i"
            persistenceService.insertLibrary(path = randomFile(), data = LibraryData(platform, name))
        }

        val executor = Executors.newFixedThreadPool(10)
        val context = executor.asCoroutineDispatcher()
        runBlocking {
            (0..numGames-1).map {
                async(context) {
                    val providerTypes = mutableListOf(*GameProviderType.values())
                    persistenceService.insertGame(
                        metaData = randomMetaData(libraries.randomElement().id),
                        providerData = List(rnd.nextInt(GameProviderType.values().size + 1)) {
                            val type = providerTypes.randomElement()
                            providerTypes -= type
                            randomProviderData(type)
                        },
                        userData = null
                    )
                }
            }.forEach { it.await() }
        }
        executor.shutdownNow()
    }

    private fun randomProviderData(type: GameProviderType) = ProviderData(
        header = randomProviderHeader(type),
        gameData = randomGameData(),
        imageUrls = imageUrls(type)
    )

    private fun randomProviderHeader(type: GameProviderType) = ProviderHeader(
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