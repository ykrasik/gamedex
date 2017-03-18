package com.gitlab.ykrasik.gamedex

import com.gitlab.ykrasik.gamedex.Main
import javafx.application.Application
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
    class TestApplication : App(TestView::class) {
        init {
            FX.dicontainer = Main.GuiceDiContainer
        }
    }

    class TestView : View("Test") {
        override val root: Parent = vbox {
            initializer()
        }
    }
    
    init {
        initializer = this::init
        Application.launch(TestApplication::class.java)
    }

    protected abstract fun init(): Unit

    private companion object {
        var initializer: () -> Unit = { }
    }
}