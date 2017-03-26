package com.gitlab.ykrasik.gamedex.ui.view.fragment

import com.gitlab.ykrasik.gamedex.common.util.clearSelection
import com.gitlab.ykrasik.gamedex.provider.DataProviderInfo
import com.gitlab.ykrasik.gamedex.provider.ProviderSearchResult
import com.gitlab.ykrasik.gamedex.provider.SearchContext
import com.gitlab.ykrasik.gamedex.ui.cancelButton
import com.gitlab.ykrasik.gamedex.ui.customColumn
import com.gitlab.ykrasik.gamedex.ui.fadeOnImageChange
import com.gitlab.ykrasik.gamedex.ui.okButton
import javafx.beans.property.ReadOnlyObjectProperty
import javafx.collections.ObservableList
import javafx.geometry.HPos
import javafx.geometry.VPos
import javafx.scene.control.TableCell
import javafx.scene.control.TableView
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.Priority
import javafx.scene.text.Font
import kotlinx.coroutines.experimental.javafx.JavaFx
import kotlinx.coroutines.experimental.run
import tornadofx.*

/**
 * User: ykrasik
 * Date: 02/01/2017
 * Time: 15:54
 */
class ChooseSearchResultFragment(
    private val context: SearchContext,
    private val info: DataProviderInfo,
    private val searchResults: ObservableList<ProviderSearchResultView>
) : Fragment("Choose Search Result for '${context.searchedName}'") {
    private var tableView: TableView<ProviderSearchResultView> by singleAssign()

    private var accept = false

    override val root = borderpane {
        paddingAll = 20
        top {
            gridpane {
                label(context.path.path) {
                    gridpaneConstraints { vAlignment = VPos.TOP; hAlignment = HPos.LEFT }
                    font = Font.font(20.0)
                }
                region { gridpaneConstraints { columnIndex = 1; rowSpan = 2; hGrow = Priority.ALWAYS } }
                imageview {
                    gridpaneConstraints { columnIndex = 2; vAlignment = VPos.TOP; hAlignment = HPos.RIGHT }
                    image = info.logo
                    fitHeight = 100.0
                    fitWidth = 100.0
                    isPreserveRatio = true
                }
                label(context.searchedName) {
                    gridpaneConstraints { rowIndex = 1; vAlignment = VPos.BOTTOM; hAlignment = HPos.LEFT }
                    font = Font.font(20.0)
                }
                label("Search results: ${searchResults.size}.") {
                    gridpaneConstraints { rowIndex = 1; columnIndex = 2; vAlignment = VPos.BOTTOM; hAlignment = HPos.RIGHT }
                    font = Font.font(20.0)
                }
                paddingBottom = 10
            }
        }
        center {
            tableView = tableview(searchResults) {
                val indexColumn = makeIndexColumn()
                customColumn("Thumbnail", ProviderSearchResultView::thumbnail) {
                    object : TableCell<ProviderSearchResultView, ReadOnlyObjectProperty<Image>>() {
                        private val imageView = ImageView().fadeOnImageChange()
                        init { graphic = imageView }
                        override fun updateItem(thumbnail: ReadOnlyObjectProperty<Image>?, empty: Boolean) {
                            if (empty) {
                                imageView.imageProperty().unbind()
                            } else {
                                imageView.imageProperty().cleanBind(thumbnail!!)
                            }
                        }
                    }
                }
                column("Name", ProviderSearchResultView::name)
                column("Release Date", ProviderSearchResultView::releaseDate)
                column("Score", ProviderSearchResultView::score)

                val minTableWidth = contentColumns.fold(indexColumn.widthProperty().subtract(10)) { binding, column ->
                    binding.add(column.widthProperty())
                }
                minWidthProperty().bind(minTableWidth)

                onUserSelect(clickCount = 2) { close(accept = true) }
            }
            // FIXME: Add a search-again-with-new-name button.
        }
        bottom {
            buttonbar {
                paddingTop = 20
                okButton { setOnAction { close(accept = true) } }
                cancelButton { setOnAction { close(accept = false) } }
                button("Proceed Without") {
                    setOnAction {
                        tableView.clearSelection()
                        close(accept = true)
                    }
                }
            }
        }
    }

    override fun onDock() {
        tableView.resizeColumnsToFitContent()
        modalStage!!.minWidthProperty().bind(tableView.minWidthProperty().add(60))
    }

    suspend fun show(): ProviderSearchResultView? = run(JavaFx) {
        openModal(block = true)
        if (accept) {
            tableView.selectedItem
        } else {
            null
        }
    }

    private fun close(accept: Boolean) {
        this.accept = accept
        close()
    }
}

class ProviderSearchResultView(
    val searchResult: ProviderSearchResult,
    val thumbnail: ReadOnlyObjectProperty<Image>
) {
    val name get() = searchResult.name
    val releaseDate get() = searchResult.releaseDate
    val score get() = searchResult.score

    override fun toString() = searchResult.toString()
}