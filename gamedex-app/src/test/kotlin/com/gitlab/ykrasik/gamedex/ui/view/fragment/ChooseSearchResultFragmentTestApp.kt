package com.gitlab.ykrasik.gamedex.ui.view.fragment

import com.gitlab.ykrasik.gamedex.BaseTestApp
import com.gitlab.ykrasik.gamedex.common.testkit.*
import com.gitlab.ykrasik.gamedex.provider.ProviderSearchResult
import com.gitlab.ykrasik.gamedex.provider.giantbomb.GiantBombDataProvider
import javafx.beans.property.SimpleObjectProperty
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
            val searchedName = randomName()
            val path = randomFile()
            val results = List(10) { ProviderSearchResultWithThumbnail(ProviderSearchResult(
                name = randomName(),
                releaseDate = randomLocalDate(),
                score = randomScore(),
                thumbnailUrl = randomUrl(),
                apiUrl = randomUrl()
            ), SimpleObjectProperty(TestImages.randomImage())) }.observable()
            println("Result: " + ChooseSearchResultFragment(searchedName, path, GiantBombDataProvider.info, results, true).show())
            System.exit(0)
        }
    }

    @JvmStatic fun main(args: Array<String>) {  }
}