package com.gitlab.ykrasik.gamedex.ui.view.fragment

import com.gitlab.ykrasik.gamedex.common.util.clearSelection
import com.gitlab.ykrasik.gamedex.common.util.sizeProperty
import com.gitlab.ykrasik.gamedex.provider.DataProviderInfo
import com.gitlab.ykrasik.gamedex.provider.ProviderSearchResult
import com.gitlab.ykrasik.gamedex.provider.SearchContext
import com.gitlab.ykrasik.gamedex.ui.cancelButton
import com.gitlab.ykrasik.gamedex.ui.okButton
import com.gitlab.ykrasik.gamedex.ui.padding
import javafx.collections.ObservableList
import javafx.scene.control.TableCell
import javafx.scene.control.TableView
import javafx.scene.image.ImageView
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
) : Fragment("Choose Search Result") {
    private var tableView: TableView<ProviderSearchResultView> by singleAssign()

    private var accept = false

    override val root = borderpane {
        minWidth = 620.0 // TODO: Make it fit the content.
        paddingAll = 20
        top {
            vbox {
                label("Path: ${context.path.path}") {
                    paddingBottom = 10
                }
                imageview {
                    image = info.logo
                    fitHeight = 200.0
                    fitWidth = 500.0
                    isPickOnBounds = true   // TODO: What is this?
                    isPreserveRatio = true
                }
                label {
                    padding { top = 10; bottom = 10 }
                    textProperty().bind(
                        searchResults.sizeProperty().asString("Search results for '${context.searchedName}': %d.")
                    )
                }
            }
        }
        center {
            tableView = tableview(searchResults) {
                makeIndexColumn()
                column("Thumbnail", ProviderSearchResultView::thumbnail) {
                    setCellFactory {
                        object : TableCell<ProviderSearchResultView, ImageView>() {
                            override fun updateItem(thumbnail: ImageView?, empty: Boolean) {
                                if (empty) graphic = null else graphic = thumbnail
                            }
                        }
                    }
                }
                column("Name", ProviderSearchResultView::name)
                column("Release Date", ProviderSearchResultView::releaseDate)
                column("Score", ProviderSearchResultView::score)

                setOnMouseClicked { e ->
                    if (e.clickCount == 2) {
                        val selectedItem = selectionModel.selectedItem
                        if (selectedItem != null) {
                            close(accept = true)
                        }
                    }
                }
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
    val thumbnail: ImageView
) {
    val name get() = searchResult.name
    val releaseDate get() = searchResult.releaseDate
    val score get() = searchResult.score

    override fun toString() = searchResult.toString()
}