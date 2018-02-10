package com.gitlab.ykrasik.gamedex

import com.gitlab.ykrasik.gamedex.ui.view.preloader.PreloaderView
import com.gitlab.ykrasik.gamedex.util.UiLogAppender
import javafx.application.Application
import org.slf4j.bridge.SLF4JBridgeHandler
import tornadofx.App

/**
 * User: ykrasik
 * Date: 08/10/2016
 * Time: 21:40
 */
class Main : App(PreloaderView::class) {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SLF4JBridgeHandler.removeHandlersForRootLogger()
            SLF4JBridgeHandler.install()
            UiLogAppender.init()

            Application.launch(Main::class.java, *args)
        }
    }
}