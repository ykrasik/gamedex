package com.gitlab.ykrasik.gamedex.ui.view.fragment

import com.gitlab.ykrasik.gamedex.BaseTestApp
import com.gitlab.ykrasik.gamedex.common.datamodel.Game
import com.gitlab.ykrasik.gamedex.common.datamodel.ImageIds
import com.gitlab.ykrasik.gamedex.common.testkit.TestImages
import com.gitlab.ykrasik.gamedex.common.testkit.rnd
import com.gitlab.ykrasik.gamedex.core.ImageLoader
import com.gitlab.ykrasik.gamedex.module.GuiceDiContainer
import com.gitlab.ykrasik.gamedex.randomGameData
import com.gitlab.ykrasik.gamedex.randomMetaData
import com.gitlab.ykrasik.gamedex.randomProviderData
import com.google.inject.AbstractModule
import com.nhaarman.mockito_kotlin.anyOrNull
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
            on { fetchImage(anyOrNull()) }.thenAnswer { SimpleObjectProperty(TestImages.randomImage()) }
        }

        return GuiceDiContainer(listOf(object : AbstractModule() {
            override fun configure() {
                bind(ImageLoader::class.java).toInstance(imageLoader)
            }
        }))
    }

    override fun init() {
        val game = Game(
            id = rnd.nextInt(),
            metaData = randomMetaData(),
            gameData = randomGameData(),
            providerData = randomProviderData(),
            imageIds = ImageIds(
                thumbnailId = rnd.nextInt(),
                posterId = rnd.nextInt(),
                screenshotIds = List(rnd.nextInt(10)) { rnd.nextInt() }
            )
        )
        println("Result: " + GameDetailsFragment(game, displayVideos = false).show())
        System.exit(0)
    }

    @JvmStatic fun main(args: Array<String>) {}
}