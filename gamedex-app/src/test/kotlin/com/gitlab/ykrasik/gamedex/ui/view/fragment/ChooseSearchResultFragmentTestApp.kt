package com.gitlab.ykrasik.gamedex.ui.view.fragment

import com.gitlab.ykrasik.gamedex.BaseTestApp
import com.gitlab.ykrasik.gamedex.provider.giantbomb.GiantBombDataProvider
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.launch
import tornadofx.observable

/**
 * User: ykrasik
 * Date: 12/03/2017
 * Time: 13:05
 */
object ChooseSearchResultFragmentTestApp : BaseTestApp() {
    override fun init() {
        launch(CommonPool) {
            println("Result: " + ChooseSearchResultFragment(randomSearchContext(), GiantBombDataProvider.info, randomSearchResults(10).observable()).show())
            System.exit(0)
        }
    }

    @JvmStatic fun main(args: Array<String>) {  }
}