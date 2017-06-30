package com.gitlab.ykrasik.gamedex.ui.view.game.search

import com.gitlab.ykrasik.gamedex.ProviderSearchResult
import com.gitlab.ykrasik.gamedex.Score
import com.gitlab.ykrasik.gamedex.core.ImageLoader
import com.gitlab.ykrasik.gamedex.core.SearchChooser
import com.gitlab.ykrasik.gamedex.ui.imageViewColumn
import com.gitlab.ykrasik.gamedex.ui.minWidthFitContent
import com.gitlab.ykrasik.gamedex.ui.theme.CommonStyle
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.value.ObservableValue
import javafx.collections.ObservableList
import tornadofx.*

/**
 * User: ykrasik
 * Date: 09/06/2017
 * Time: 21:29
 */
class SearchResultsContentFragment(results: ObservableList<ProviderSearchResult>, close: (SearchChooser.Choice) -> Unit) : Fragment() {
    private val imageLoader: ImageLoader by di()

    val minTableWidth = SimpleDoubleProperty()

    override val root = tableview(results) {
        val indexColumn = makeIndexColumn().apply { addClass(CommonStyle.centered) }
        imageViewColumn("Thumbnail", fitWidth = 200.0, fitHeight = 200.0, isPreserveRatio = true) { result ->
            imageLoader.downloadImage(result.thumbnailUrl)
        }
        column("Name", ProviderSearchResult::name)
        column("Release Date", ProviderSearchResult::releaseDate) { addClass(CommonStyle.centered) }
        column<ProviderSearchResult, String>("Critic Score") { toScoreDisplay(it.value.criticScore, "critics") }.apply {
            addClass(CommonStyle.centered)
        }
        column<ProviderSearchResult, String>("User Score") { toScoreDisplay(it.value.userScore, "users") }.apply {
            addClass(CommonStyle.centered)
        }

        minWidthFitContent(indexColumn)

        onUserSelect(clickCount = 2) { close(SearchChooser.Choice.ExactMatch(this@tableview.selectedItem!!)) }
    }

    private fun toScoreDisplay(score: Score?, type: String): ObservableValue<String> =
        (if (score == null) "" else "${score.score}   |   ${score.numReviews} $type").toProperty()
}