package com.gitlab.ykrasik.gamedex.ui.view.report

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.controller.ReportsController
import com.gitlab.ykrasik.gamedex.ui.skipFirstTime
import javafx.scene.Node
import tornadofx.View

/**
 * User: ykrasik
 * Date: 11/06/2017
 * Time: 09:48
 */
abstract class ReportView<T>(title: String, icon: Node) : View(title, icon) {
    abstract val ongoingReport: ReportsController.OngoingReport<T>

    override fun onDock() {
        // This is called on application startup, but we don't want to start any calculations before
        // the user explicitly entered the reports screen. A bit of a hack.
        skipFirstTime { ongoingReport.start() }
    }

    override fun onUndock() = ongoingReport.stop()
}

typealias Report<T> = Map<Game, T>