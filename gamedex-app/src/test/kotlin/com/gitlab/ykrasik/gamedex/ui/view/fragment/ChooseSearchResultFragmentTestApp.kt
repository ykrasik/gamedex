package com.gitlab.ykrasik.gamedex.ui.view.fragment

import com.gitlab.ykrasik.gamedex.BaseTestApp
import com.gitlab.ykrasik.gamedex.common.datamodel.DataProviderType
import com.gitlab.ykrasik.gamedex.common.util.toFile
import com.gitlab.ykrasik.gamedex.provider.DataProviderInfo
import com.gitlab.ykrasik.gamedex.provider.SearchContext
import com.gitlab.ykrasik.gamedex.ui.UIResources
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.launch

/**
 * User: ykrasik
 * Date: 12/03/2017
 * Time: 13:05
 */
object ChooseSearchResultFragmentTestApp : BaseTestApp() {
    override fun init() {
        val context = SearchContext("Assassin's Creed", "somePath".toFile())
        val info = DataProviderInfo("Some Provider", DataProviderType.GiantBomb, UIResources.Images.notAvailable)
        launch(CommonPool) {
            println("Result: " + ChooseSearchResultFragment(context, info, testSearchResults).show())
            System.exit(0)
        }
    }

    @JvmStatic fun main(args: Array<String>) {  }
}