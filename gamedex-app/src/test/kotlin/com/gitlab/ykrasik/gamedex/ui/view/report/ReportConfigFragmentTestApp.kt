package com.gitlab.ykrasik.gamedex.ui.view.report

import com.gitlab.ykrasik.gamedex.BaseFragmentTestApp
import com.gitlab.ykrasik.gamedex.core.Filter

/**
 * User: ykrasik
 * Date: 18/06/2017
 * Time: 11:11
 */
object ReportConfigFragmentTestApp : BaseFragmentTestApp() {
    override fun init() {
        println("Result: " + ReportConfigFragment(ReportConfig("", Filter.`true`, emptyList())).show())
    }

    @JvmStatic fun main(args: Array<String>) {  }
}