package com.gitlab.ykrasik.gamedex

import com.github.ykrasik.gamedex.common.module.CommonModule
import com.gitlab.ykrasik.gamedex.core.module.CoreModule
import com.gitlab.ykrasik.gamedex.core.ui.view.MainView
import com.gitlab.ykrasik.gamedex.core.ui.view.Styles
import com.gitlab.ykrasik.gamedex.persistence.module.PersistenceModule
import com.gitlab.ykrasik.gamedex.provider.giantbomb.module.GiantBombProviderModule
import com.gitlab.ykrasik.gamedex.provider.module.DataProviderModule
import com.google.inject.Guice
import com.google.inject.Module
import javafx.application.Application
import javafx.stage.Stage
import tornadofx.App
import tornadofx.DIContainer
import tornadofx.FX
import kotlin.reflect.KClass

/**
 * User: ykrasik
 * Date: 08/10/2016
 * Time: 21:40
 */
class Main : App(MainView::class, Styles::class) {
    init {
        // Import a file based stylesheet
//        importStylesheet(Styles::class)

        FX.dicontainer = GuiceDiContainer(
            CommonModule(),
            PersistenceModule(),
            CoreModule(),
            DataProviderModule(),
            GiantBombProviderModule()
        )
    }

    override fun start(stage: Stage) {
        stage.isMaximized = true
        super.start(stage)
    }

    private class GuiceDiContainer(vararg modules: Module) : DIContainer {
        private val guice = Guice.createInjector(com.google.inject.Stage.PRODUCTION, *modules)
        override fun <T : Any> getInstance(type: KClass<T>) = guice.getInstance(type.java)
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            Application.launch(Main::class.java, *args)
        }
    }
}