package com.gitlab.ykrasik.gamedex.ui.view.report

import com.gitlab.ykrasik.gamedex.BaseFragmentTestApp

/**
 * User: ykrasik
 * Date: 18/06/2017
 * Time: 11:11
 */
object ReportConfigFragmentTestApp : BaseFragmentTestApp() {
    override fun init() {
        println("Result: " + ReportConfigFragment(ReportConfig()).show())
    }

    @JvmStatic fun main(args: Array<String>) {  }
}