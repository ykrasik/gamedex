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
import com.gitlab.ykrasik.gamedex.util.stringConfig
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doAnswer
import com.nhaarman.mockito_kotlin.mock
import kotlinx.coroutines.experimental.asCoroutineDispatcher
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.runBlocking
import java.util.concurrent.Executors

/**
 * User: ykrasik
 * Date: 18/03/2017
 * Time: 21:59
 */
object TestApplication {
    val giantBombServer = GiantBombFakeServer(9001, apiKey = "valid")
    val igdbServer = IgdbFakeServer(9002, apiKey = "valid")

    @JvmStatic fun main(args: Array<String>) {
        appConfig = appConfig
            .withValue("gameDex.persistence.dbUrl", stringConfig("jdbc:h2:./test"))
            .withValue("gameDex.provider.giantBomb.endpoint", stringConfig(giantBombServer.endpointUrl))
            .withValue("gameDex.provider.igdb.endpoint", stringConfig(igdbServer.endpointUrl))
            .withValue("gameDex.provider.igdb.baseImageUrl", stringConfig(igdbServer.baseImageUrl))

        newDirectoryDetector = mock {
            on { detectNewDirectories(any(), any()) } doAnswer { List(rnd.nextInt(4)) { randomFile() } }
        }

        // Pre-Load test images
        TestImages

        giantBombServer.start().use {
            igdbServer.start().use {
                generateDb()

                Main.main(args)
            }
        }
    }

    private fun generateDb() {
        val numGames = 500

        val guice = GuiceDiContainer(listOf(PersistenceModule, ConfigModule))
        val persistenceService = guice.getInstance(PersistenceService::class)
        persistenceService.dropDb()

        val libraries = (1..5).zip(listOf(Platform.pc, Platform.android, Platform.mac, Platform.excluded, Platform.pc)).map { (i, platform) ->
            val name = "lib$i"
            persistenceService.insertLibrary(path = randomFile(), data = LibraryData(platform, name))
        }.filter { it.platform != Platform.excluded }

        val executor = Executors.newFixedThreadPool(10)
        val context = executor.asCoroutineDispatcher()
        runBlocking {
            (0 until numGames).map {
                async(context) {
                    val providerIds = mutableListOf(giantBombServer.providerId, igdbServer.providerId)
                    persistenceService.insertGame(
                        metadata = randomMetadata(libraries.randomElement().id),
                        providerData = List(rnd.nextInt(providerIds.size + 1)) {
                            val providerId = providerIds.randomElement()
                            providerIds -= providerId
                            randomProviderData(providerId)
                        },
                        userData = null
                    )
                }
            }.forEach { it.await() }
        }
        executor.shutdownNow()
    }

    private fun randomProviderData(id: ProviderId) = ProviderData(
        header = randomProviderHeader(id, apiUrl(id)),
        gameData = randomGameData(),
        imageUrls = imageUrls(id)
    )

    private fun apiUrl(id: ProviderId) = when (id) {
        giantBombServer.providerId -> giantBombServer.apiDetailsUrl
        igdbServer.providerId -> igdbServer.detailsUrl(rnd.nextInt())
        else -> error("Invalid providerId: $id")
    }

    private fun imageUrls(id: ProviderId) = when (id) {
        giantBombServer.providerId -> randomGiantBombImageUrls()
        igdbServer.providerId -> randomIgdbImageUrls()
        else -> error("Invalid providerId: $id")
    }

    private fun randomGiantBombImageUrls() = ImageUrls(
        thumbnailUrl = "${giantBombServer.thumbnailUrl}/${randomString()}.jpg".sometimesNull(),
        posterUrl = "${giantBombServer.superUrl}/${randomString()}.jpg".sometimesNull(),
        screenshotUrls = List(rnd.nextInt(10)) { "${giantBombServer.screenshotUrl}/${randomString()}.jpg" }
    )

    private fun randomIgdbImageUrls() = ImageUrls(
        thumbnailUrl = "${igdbServer.thumbnailUrl}/${randomString()}.png".sometimesNull(),
        posterUrl = "${igdbServer.posterUrl}/${randomString()}.png".sometimesNull(),
        screenshotUrls = List(rnd.nextInt(10)) { "${igdbServer.screenshotUrl}/${randomString()}.png" }
    )

    // 9/10 chance.
    private fun String.sometimesNull() = if (rnd.nextInt(10) < 9) this else null
}