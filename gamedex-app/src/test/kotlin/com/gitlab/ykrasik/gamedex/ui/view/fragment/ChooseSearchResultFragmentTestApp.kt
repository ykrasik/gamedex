package com.gitlab.ykrasik.gamedex.ui.view.fragment

import com.gitlab.ykrasik.gamedex.common.datamodel.DataProviderType
import com.gitlab.ykrasik.gamedex.common.util.getResourceAsByteArray
import com.gitlab.ykrasik.gamedex.common.util.toFile
import com.gitlab.ykrasik.gamedex.common.util.toImageView
import com.gitlab.ykrasik.gamedex.provider.DataProviderInfo
import com.gitlab.ykrasik.gamedex.provider.ProviderSearchResult
import com.gitlab.ykrasik.gamedex.provider.SearchContext
import com.gitlab.ykrasik.gamedex.ui.UIResources
import com.gitlab.ykrasik.gamedex.ui.view.BaseTestApp
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.launch
import org.joda.time.LocalDate
import tornadofx.observable

/**
 * User: ykrasik
 * Date: 12/03/2017
 * Time: 13:05
 */
class ChooseSearchResultFragmentTestApp : BaseTestApp() {
    override fun init() {
        val context = SearchContext("Assassin's Creed", "somePath".toFile())
        val info = DataProviderInfo("Some Provider", DataProviderType.GiantBomb, UIResources.Images.notAvailable)
        val results = listOf(
            ProviderSearchResultView(ProviderSearchResult(
                name = "Assassin's Creed",
                releaseDate = LocalDate.parse("2007-11-13"),
                score = 89.1,
                thumbnailUrl = "http://www.giantbomb.com/api/image/scale_avatar/2843355-ac.jpg",
                apiUrl = "http://www.giantbomb.com/api/game/3030-2950/"
            ), getResourceAsByteArray("ac1.jpg").toImageView()),
            ProviderSearchResultView(ProviderSearchResult(
                name = "Assassin's Creed II",
                releaseDate = LocalDate.parse("2009-11-17"),
                score = 86.7,
                thumbnailUrl = "http://www.giantbomb.com/api/image/scale_avatar/2392977-assassins_creed_ii_05_artwork.jpg",
                apiUrl = "http://www.giantbomb.com/api/game/3030-22928/"
            ), getResourceAsByteArray("ac2.jpg").toImageView()),
            ProviderSearchResultView(ProviderSearchResult(
                name = "Assassin's Creed: Brotherhood",
                releaseDate = LocalDate.parse("2010-11-16"),
                score = 84.3,
                thumbnailUrl = "http://www.giantbomb.com/api/image/scale_avatar/2843337-acbro.jpg",
                apiUrl = "http://www.giantbomb.com/api/game/3030-31001/"
            ), getResourceAsByteArray("ac3.jpg").toImageView())

        ).observable()
        launch(CommonPool) {
            println("Result: " + ChooseSearchResultFragment(context, info, results).show())
            System.exit(0)
        }
    }

    companion object {
        @JvmStatic fun main(args: Array<String>) { ChooseSearchResultFragmentTestApp() }
    }
}