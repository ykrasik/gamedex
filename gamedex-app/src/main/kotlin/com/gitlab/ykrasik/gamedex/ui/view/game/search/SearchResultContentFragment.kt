package com.gitlab.ykrasik.gamedex.ui.view.game.search

import com.gitlab.ykrasik.gamedex.ProviderSearchResult
import com.gitlab.ykrasik.gamedex.Score
import com.gitlab.ykrasik.gamedex.core.ImageLoader
import com.gitlab.ykrasik.gamedex.core.SearchChooser
import com.gitlab.ykrasik.gamedex.ui.customColumn
import com.gitlab.ykrasik.gamedex.ui.fadeOnImageChange
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.value.ObservableValue
import javafx.collections.ObservableList
import javafx.geometry.Pos
import javafx.scene.control.TableCell
import javafx.scene.image.ImageView
import tornadofx.*

/**
 * User: ykrasik
 * Date: 09/06/2017
 * Time: 21:29
 */
class SearchResultContentFragment(results: ObservableList<ProviderSearchResult>,  close: (SearchChooser.Choice) -> Unit) : Fragment() {
    private val imageLoader: ImageLoader by di()

    val minTableWidth = SimpleDoubleProperty()

    override val root = tableview(results) {
        val indexColumn = makeIndexColumn().apply { addClass(Style.centered) }
        customColumn("Thumbnail", ProviderSearchResult::thumbnailUrl) {
            object : TableCell<ProviderSearchResult, String?>() {
                private val imageView = ImageView().apply {
                    fadeOnImageChange()
                    fitHeight = 200.0
                    fitWidth = 200.0
                    isPreserveRatio = true
                }

                init {
                    addClass(Style.centered)
                    graphic = imageView
                }

                override fun updateItem(thumbnailUrl: String?, empty: Boolean) {
                    if (empty) {
                        imageView.imageProperty().unbind()
                        imageView.image = null
                    } else {
                        val thumbnail = imageLoader.downloadImage(thumbnailUrl)
                        imageView.imageProperty().cleanBind(thumbnail)
                    }
                }
            }
        }
        column("Name", ProviderSearchResult::name)
        column("Release Date", ProviderSearchResult::releaseDate) { addClass(Style.centered) }
        column<ProviderSearchResult, String>("Critic Score") { toScoreDisplay(it.value.criticScore, "critics") }.apply {
            addClass(Style.centered)
        }
        column<ProviderSearchResult, String>("User Score") { toScoreDisplay(it.value.userScore, "users") }.apply {
            addClass(Style.centered)
        }

        minTableWidth.bind(contentColumns.fold(indexColumn.widthProperty().subtract(10)) { binding, column ->
            binding.add(column.widthProperty())
        })
        minWidthProperty().bind(minTableWidth)

        onUserSelect(clickCount = 2) { close(SearchChooser.Choice.ExactMatch(this@tableview.selectedItem!!)) }
    }

    private fun toScoreDisplay(score: Score?, type: String): ObservableValue<String> =
        (if (score == null) "" else "${score.score}   |   ${score.numReviews} $type").toProperty()

    class Style : Stylesheet() {
        companion object {
            val centered by cssclass()

            init {
                importStylesheet(Style::class)
            }
        }

        init {
            centered {
                alignment = Pos.TOP_CENTER
            }
        }
    }
}