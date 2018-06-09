/****************************************************************************
 * Copyright (C) 2016-2018 Yevgeny Krasik                                   *
 *                                                                          *
 * Licensed under the Apache License, Version 2.0 (the "License");          *
 * you may not use this file except in compliance with the License.         *
 * You may obtain a copy of the License at                                  *
 *                                                                          *
 * http://www.apache.org/licenses/LICENSE-2.0                               *
 *                                                                          *
 * Unless required by applicable law or agreed to in writing, software      *
 * distributed under the License is distributed on an "AS IS" BASIS,        *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. *
 * See the License for the specific language governing permissions and      *
 * limitations under the License.                                           *
 ****************************************************************************/

package com.gitlab.ykrasik.gamedex.javafx

import com.gitlab.ykrasik.gamedex.core.api.image.ImageRepository
import com.gitlab.ykrasik.gamedex.core.module.CoreModule
import com.gitlab.ykrasik.gamedex.javafx.module.GuiceDiContainer
import com.gitlab.ykrasik.gamedex.javafx.module.JavaFxModule
import com.google.inject.*
import com.google.inject.spi.Elements
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import javafx.application.Application
import javafx.beans.property.SimpleObjectProperty
import javafx.scene.image.Image
import tornadofx.*
import java.io.ByteArrayInputStream
import kotlin.reflect.KClass

/**
 * User: ykrasik
 * Date: 10/06/2017
 * Time: 11:36
 */
@Suppress("UNCHECKED_CAST")
abstract class BaseTestApp<in T : UIComponent>(view: KClass<out T>) {
    init {
        // Use an in-memory test db.
        System.setProperty("gameDex.persistence.dbUrl", "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1")

        // Mock imageLoader.
        val coreModuleWithoutImageRepo = Elements.getModule(Elements.getElements(CoreModule).filter {
            it is Binding<*> && it.key != Key.get(ImageRepository::class.java)
        })
        val mockImageRepoModule = object : AbstractModule() {
            override fun configure() {
                bind(ImageRepository::class.java).toInstance(mock {
                    on { downloadImage(any()) }.thenAnswer { SimpleObjectProperty(randomImage()) }
                    on { fetchImage(any(), any(), any()) }.thenAnswer { SimpleObjectProperty(randomImage()) }
                })
            }
        }
        FX.dicontainer = GuiceDiContainer(
            Guice.createInjector(Stage.PRODUCTION, JavaFxModule, coreModuleWithoutImageRepo, mockImageRepoModule)
        )

        Companion.view = view
        initializer = { v -> init((v as T)) }
        Application.launch(TestLauncher::class.java)
    }

    protected abstract fun init(view: T)

    private fun randomImage(): Image = Image(ByteArrayInputStream(com.gitlab.ykrasik.gamedex.test.randomImage()))

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