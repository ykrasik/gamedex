package com.gitlab.ykrasik.gamedex

import com.gitlab.ykrasik.gamedex.module.GuiceDiContainer
import com.gitlab.ykrasik.gamedex.ui.view.Styles
import com.gitlab.ykrasik.gamedex.ui.view.fragment.GameDetailsFragment
import javafx.application.Application
import javafx.scene.Parent
import tornadofx.*

/**
 * User: ykrasik
 * Date: 12/03/2017
 * Time: 13:53
 */
abstract class BaseTestApp {
    init {
        FX.dicontainer = diContainer()
        initializer = this::init
        Application.launch(TestApplication::class.java)
    }

    open fun diContainer(): DIContainer = GuiceDiContainer()

    protected abstract fun init(): Unit

    companion object {
        var initializer: () -> Unit = { }

        class TestApplication : App(TestView::class, Styles::class) {
            init {
                importStylesheet(GameDetailsFragment.Style::class)
            }
        }

        class TestView : View("Test") {
            override val root: Parent = vbox {
                initializer()
            }
        }
    }
}