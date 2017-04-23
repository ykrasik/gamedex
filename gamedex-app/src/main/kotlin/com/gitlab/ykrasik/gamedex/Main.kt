package com.gitlab.ykrasik.gamedex

import com.gitlab.ykrasik.gamedex.ui.view.PreloaderView
import com.gitlab.ykrasik.gamedex.ui.view.Styles
import javafx.application.Application
import tornadofx.App

/**
 * User: ykrasik
 * Date: 08/10/2016
 * Time: 21:40
 */
class Main : App(PreloaderView::class, Styles::class) {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            Application.launch(Main::class.java, *args)
        }
    }
}