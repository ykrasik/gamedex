package com.gitlab.ykrasik.gamedex.ui.view.fragment

import com.gitlab.ykrasik.gamedex.BaseTestApp
import com.gitlab.ykrasik.gamedex.Main
import com.gitlab.ykrasik.gamedex.common.testkit.*
import com.gitlab.ykrasik.gamedex.core.ImageLoader
import com.gitlab.ykrasik.gamedex.provider.ChooseSearchResultData
import com.gitlab.ykrasik.gamedex.provider.ProviderSearchResult
import com.gitlab.ykrasik.gamedex.provider.giantbomb.GiantBombDataProvider
import com.google.inject.AbstractModule
import com.nhaarman.mockito_kotlin.anyOrNull
import com.nhaarman.mockito_kotlin.mock
import javafx.beans.property.SimpleObjectProperty
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.launch
import tornadofx.DIContainer

/**
 * User: ykrasik
 * Date: 12/03/2017
 * Time: 13:05
 */
object ChooseSearchResultFragmentTestApp : BaseTestApp() {
    override fun diContainer(): DIContainer {
        val imageLoader = mock<ImageLoader> {
            on { downloadImage(anyOrNull()) }.thenAnswer { SimpleObjectProperty(TestImages.randomImage()) }
        }

        return Main.GuiceDiContainer(object : AbstractModule() {
            override fun configure() {
                bind(ImageLoader::class.java).toInstance(imageLoader)
            }
        })
    }

    override fun init() {
        launch(CommonPool) {
            val data = ChooseSearchResultData(
                name = randomName(),
                path = randomFile(),
                info = GiantBombDataProvider.info,
                searchResults = List(10) {
                    ProviderSearchResult(
                        name = randomName(),
                        releaseDate = randomLocalDate(),
                        score = randomScore(),
                        thumbnailUrl = randomUrl(),
                        apiUrl = randomUrl()
                    )
                },
                canProceedWithout = true
            )
            println("Result: " + ChooseSearchResultFragment(data).show())
            System.exit(0)
        }
    }

    @JvmStatic fun main(args: Array<String>) {}
}