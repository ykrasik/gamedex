package com.gitlab.ykrasik.gamedex.ui.view.report

import com.gitlab.ykrasik.gamedex.BaseFragmentTestApp
import com.gitlab.ykrasik.gamedex.core.ReportRule

/**
 * User: ykrasik
 * Date: 18/06/2017
 * Time: 11:11
 */
object ViolationRulesFragmentTestApp : BaseFragmentTestApp() {
    override fun init() {
        println("Result: " + ViolationRulesFragment(ReportRule.Nop()).show())
    }

    @JvmStatic fun main(args: Array<String>) {  }
}