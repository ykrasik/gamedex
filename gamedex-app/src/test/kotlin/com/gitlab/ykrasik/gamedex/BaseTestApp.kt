package com.gitlab.ykrasik.gamedex

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

    open fun diContainer(): DIContainer = Main.MainDiContainer

    protected abstract fun init(): Unit

    companion object {
        var initializer: () -> Unit = { }

        class TestApplication : App(TestView::class)

        class TestView : View("Test") {
            override val root: Parent = vbox {
                initializer()
            }
        }
    }
}