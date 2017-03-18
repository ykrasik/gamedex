package com.gitlab.ykrasik.gamedex

import com.gitlab.ykrasik.gamedex.common.module.CommonModule
import com.gitlab.ykrasik.gamedex.module.AppModule
import com.gitlab.ykrasik.gamedex.persistence.module.PersistenceModule
import com.gitlab.ykrasik.gamedex.provider.giantbomb.module.GiantBombProviderModule
import com.gitlab.ykrasik.gamedex.provider.igdb.module.IgdbProviderModule
import com.gitlab.ykrasik.gamedex.provider.module.DataProviderModule
import com.gitlab.ykrasik.gamedex.ui.view.MainView
import com.gitlab.ykrasik.gamedex.ui.view.Styles
import com.google.inject.Guice
import com.google.inject.Stage.PRODUCTION
import javafx.application.Application
import javafx.stage.Stage
import org.slf4j.bridge.SLF4JBridgeHandler
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
        FX.dicontainer = GuiceDiContainer

        SLF4JBridgeHandler.removeHandlersForRootLogger()
        SLF4JBridgeHandler.install()
    }

    override fun start(stage: Stage) {
        stage.isMaximized = true
        super.start(stage)
    }

    object GuiceDiContainer : DIContainer {
        private val injector = Guice.createInjector(PRODUCTION,
            AppModule(),
            CommonModule(),
            PersistenceModule(),
            DataProviderModule(),
            GiantBombProviderModule(),
            IgdbProviderModule()
        )

        override fun <T : Any> getInstance(type: KClass<T>): T = injector.getInstance(type.java)
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            Application.launch(Main::class.java, *args)
        }
    }
}