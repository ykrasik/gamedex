package com.gitlab.ykrasik.gamedex

import com.gitlab.ykrasik.gamedex.ui.fragment.ChooseSearchResultFragment
import com.gitlab.ykrasik.gamedex.ui.view.PreloaderView
import com.gitlab.ykrasik.gamedex.util.LoggerFactory
import com.gitlab.ykrasik.gamedex.util.UILoggerFactory
import javafx.application.Application
import org.slf4j.bridge.SLF4JBridgeHandler
import tornadofx.App
import tornadofx.importStylesheet

/**
 * User: ykrasik
 * Date: 08/10/2016
 * Time: 21:40
 */
class Main : App(PreloaderView::class) {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            // TODO: At some point I could probably bridge slf4j to the ui logger, if needed.
            SLF4JBridgeHandler.removeHandlersForRootLogger()
            SLF4JBridgeHandler.install()
            LoggerFactory = UILoggerFactory

            importStylesheet(ChooseSearchResultFragment.Style::class)

            Application.launch(Main::class.java, *args)
        }
    }
}