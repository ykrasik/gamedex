package com.gitlab.ykrasik.gamedex

import tornadofx.View
import tornadofx.vbox

/**
 * User: ykrasik
 * Date: 10/06/2017
 * Time: 12:05
 */
abstract class BaseFragmentTestApp : BaseTestApp<BaseFragmentTestApp.DummyView>(DummyView::class) {
    class DummyView : View() {
        override val root = vbox {}
    }

    override fun init(view: DummyView) {
        this@BaseFragmentTestApp.init()
        System.exit(0)
    }

    protected abstract fun init()
}