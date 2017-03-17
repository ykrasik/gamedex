package com.gitlab.ykrasik.gamedex.core

import com.gitlab.ykrasik.gamedex.provider.DataProviderInfo
import com.gitlab.ykrasik.gamedex.provider.GameSearchChooser
import com.gitlab.ykrasik.gamedex.provider.ProviderSearchResult
import com.gitlab.ykrasik.gamedex.provider.SearchContext
import com.gitlab.ykrasik.gamedex.ui.UIResources
import com.gitlab.ykrasik.gamedex.ui.view.fragment.ChooseSearchResultFragment
import com.gitlab.ykrasik.gamedex.ui.view.fragment.ProviderSearchResultView
import javafx.scene.image.ImageView
import tornadofx.observable
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 30/12/2016
 * Time: 22:03
 */
@Singleton
class GameSearchChooserImpl @Inject constructor(private val imageLoader: ImageLoader) : GameSearchChooser {
    override fun choose(info: DataProviderInfo, searchResults: List<ProviderSearchResult>, context: SearchContext): ProviderSearchResult? {
        val resultViews = searchResults.map { it.toView() }.observable()
        return ChooseSearchResultFragment(context, info, resultViews).show()?.searchResult
    }

    private fun ProviderSearchResult.toView(): ProviderSearchResultView {
        val imageView = ImageView()
        val url = thumbnailUrl
        if (url != null) {
            imageLoader.downloadImage(url, imageView)
        } else {
            // TODO: Consider moving this logic into the imageLoader.
            imageView.image = UIResources.Images.notAvailable
        }
        return ProviderSearchResultView(this, imageView)
    }
}