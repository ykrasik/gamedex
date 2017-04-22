package com.gitlab.ykrasik.gamedex.ui.view.fragment

import com.gitlab.ykrasik.gamedex.*
import com.gitlab.ykrasik.gamedex.core.ImageLoader
import com.gitlab.ykrasik.gamedex.module.GuiceDiContainer
import com.gitlab.ykrasik.gamedex.persistence.PersistenceService
import com.gitlab.ykrasik.gamedex.provider.giantbomb.module.GiantBombModule
import com.gitlab.ykrasik.gamedex.provider.igdb.module.IgdbModule
import com.gitlab.ykrasik.gamedex.test.TestImages
import com.gitlab.ykrasik.gamedex.test.randomUrl
import com.gitlab.ykrasik.gamedex.test.rnd
import com.google.inject.AbstractModule
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.anyOrNull
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import javafx.beans.property.SimpleObjectProperty
import tornadofx.DIContainer

/**
 * User: ykrasik
 * Date: 01/04/2017
 * Time: 14:06
 */
object GameDetailsFragmentTestApp : BaseTestApp() {
    override fun diContainer(): DIContainer {
        val imageLoader = mock<ImageLoader> {
            on { downloadImage(anyOrNull()) }.thenAnswer { SimpleObjectProperty(TestImages.randomImage()) }
            on { fetchImage(any(), anyOrNull()) }.thenAnswer { SimpleObjectProperty(TestImages.randomImage()) }
        }
        val persistenceService = mock<PersistenceService> {
            on { fetchAllGames() } doReturn emptyList<RawGame>()
            on { fetchAllLibraries() } doReturn emptyList<Library>()
        }

        return GuiceDiContainer(listOf(object : AbstractModule() {
            override fun configure() {
                bind(ImageLoader::class.java).toInstance(imageLoader)
                bind(PersistenceService::class.java).toInstance(persistenceService)
            }
        }, GiantBombModule, IgdbModule))
    }

    override fun init() {
        val game = Game(
            id = rnd.nextInt(),
            metaData = randomMetaData(),
            gameData = randomGameData(),
            providerData = randomProviderData(),
            imageUrls = ImageUrls(
                thumbnailUrl = randomUrl(),
                posterUrl = randomUrl(),
                screenshotUrls = List(rnd.nextInt(10)) { randomUrl() }
            )
        )
        println("Result: " + GameDetailsFragment(game, displayVideos = false).show())
        System.exit(0)
    }

    @JvmStatic fun main(args: Array<String>) {}
}