package com.gitlab.ykrasik.gamedex

import com.gitlab.ykrasik.gamedex.core.ImageLoader
import com.gitlab.ykrasik.gamedex.module.AppModule
import com.gitlab.ykrasik.gamedex.module.GuiceDiContainer
import com.gitlab.ykrasik.gamedex.test.TestImages
import com.gitlab.ykrasik.gamedex.ui.view.Styles
import com.gitlab.ykrasik.gamedex.util.appConfig
import com.google.inject.AbstractModule
import com.google.inject.Binding
import com.google.inject.Key
import com.google.inject.spi.Elements
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.anyOrNull
import com.nhaarman.mockito_kotlin.mock
import com.typesafe.config.ConfigValueFactory
import javafx.application.Application
import javafx.beans.property.SimpleObjectProperty
import javafx.scene.Parent
import tornadofx.App
import tornadofx.FX
import tornadofx.View
import tornadofx.vbox

/**
 * User: ykrasik
 * Date: 12/03/2017
 * Time: 13:53
 */
abstract class BaseTestApp {
    init {
        appConfig = appConfig
            .withValue("gameDex.persistence.dbUrl", ConfigValueFactory.fromAnyRef("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1"))

        initializer = this::init
        Application.launch(TestApplication::class.java)
    }


    protected abstract fun init(): Unit

    companion object {
        var initializer: () -> Unit = { }

        class TestApplication : App(TestView::class, Styles::class)

        class TestView : View("Test") {
            override val root: Parent = vbox {
                prepare()
                initializer()
            }

            private fun prepare() {
                val appModuleWithoutImageLoader = Elements.getModule(Elements.getElements(AppModule).filter {
                    it is Binding<*> && it.key != Key.get(ImageLoader::class.java)
                })
                val mockImageLoaderModule = object : AbstractModule() {
                    override fun configure() {
                        val imageLoader = mock<ImageLoader> {
                            on { downloadImage(anyOrNull()) }.thenAnswer { SimpleObjectProperty(TestImages.randomImage()) }
                            on { fetchImage(any(), anyOrNull(), any()) }.thenAnswer { SimpleObjectProperty(TestImages.randomImage()) }
                        }
                        bind(ImageLoader::class.java).toInstance(imageLoader)
                    }
                }
                FX.dicontainer = GuiceDiContainer(
                    GuiceDiContainer.defaultModules - AppModule + appModuleWithoutImageLoader + mockImageLoaderModule
                )
            }
        }
    }
}