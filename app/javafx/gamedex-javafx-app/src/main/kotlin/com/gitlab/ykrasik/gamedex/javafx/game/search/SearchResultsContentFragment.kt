/****************************************************************************
 * Copyright (C) 2016-2018 Yevgeny Krasik                                   *
 *                                                                          *
 * Licensed under the Apache License, Version 2.0 (the "License");          *
 * you may not use this file except in compliance with the License.         *
 * You may obtain a copy of the License at                                  *
 *                                                                          *
 * http://www.apache.org/licenses/LICENSE-2.0                               *
 *                                                                          *
 * Unless required by applicable law or agreed to in writing, software      *
 * distributed under the License is distributed on an "AS IS" BASIS,        *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. *
 * See the License for the specific language governing permissions and      *
 * limitations under the License.                                           *
 ****************************************************************************/

package com.gitlab.ykrasik.gamedex.javafx.game.search

import com.gitlab.ykrasik.gamedex.Score
import com.gitlab.ykrasik.gamedex.app.javafx.image.ImageLoader
import com.gitlab.ykrasik.gamedex.core.provider.SearchChooser
import com.gitlab.ykrasik.gamedex.javafx.CommonStyle
import com.gitlab.ykrasik.gamedex.javafx.control.imageViewColumn
import com.gitlab.ykrasik.gamedex.javafx.control.minWidthFitContent
import com.gitlab.ykrasik.gamedex.provider.ProviderSearchResult
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
        readonlyColumn("Name", ProviderSearchResult::name)
        readonlyColumn("Release Date", ProviderSearchResult::releaseDate) { addClass(CommonStyle.centered) }
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