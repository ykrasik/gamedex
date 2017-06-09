package com.gitlab.ykrasik.gamedex

import com.gitlab.ykrasik.gamedex.core.ImageLoader
import com.gitlab.ykrasik.gamedex.module.AppModule
import com.gitlab.ykrasik.gamedex.module.GuiceDiContainer
import com.gitlab.ykrasik.gamedex.test.TestImages
import com.gitlab.ykrasik.gamedex.util.appConfig
import com.gitlab.ykrasik.gamedex.util.stringConfig
import com.google.inject.AbstractModule
import com.google.inject.Binding
import com.google.inject.Key
import com.google.inject.spi.Elements
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.anyOrNull
import com.nhaarman.mockito_kotlin.mock
import javafx.application.Application
import javafx.beans.property.SimpleObjectProperty
import tornadofx.*
import kotlin.reflect.KClass

/**
 * User: ykrasik
 * Date: 10/06/2017
 * Time: 11:36
 */
abstract class BaseTestApp<in T : UIComponent>(view: KClass<out T>) {
    init {
        // Use an in-memory test db.
        appConfig = appConfig.withValue("gameDex.persistence.dbUrl", stringConfig("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1"))

        // Mock imageLoader.
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

        BaseTestApp.view = view
        BaseTestApp.initializer = { v -> init((v as T)) }
        Application.launch(TestLauncher::class.java)
    }

    protected abstract fun init(view: T): Unit

    class TestLauncher : App(TestInitialView::class)
    class TestInitialView : View() {
        override val root = run {
            val concreteView = find(view, scope)
            initializer(concreteView)
            concreteView.root
        }
    }

    companion object {
        private lateinit var view: KClass<out UIComponent>
        private lateinit var initializer: (Any) -> Unit
    }
}