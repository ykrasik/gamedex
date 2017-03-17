package com.gitlab.ykrasik.gamedex.ui.view.fragment

import com.gitlab.ykrasik.gamedex.common.util.clearSelection
import com.gitlab.ykrasik.gamedex.common.util.sizeProperty
import com.gitlab.ykrasik.gamedex.provider.DataProviderInfo
import com.gitlab.ykrasik.gamedex.provider.ProviderSearchResult
import com.gitlab.ykrasik.gamedex.provider.SearchContext
import com.gitlab.ykrasik.gamedex.ui.padding
import javafx.collections.ObservableList
import javafx.scene.control.ButtonBar
import javafx.scene.control.TableCell
import javafx.scene.control.TableView
import javafx.scene.image.ImageView
import kotlinx.coroutines.experimental.javafx.JavaFx
import kotlinx.coroutines.experimental.runBlocking
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
                            // TODO: Select clicked item & exit.
                        }
                    }
                }
            }
            // FIXME: Add a search-again-with-new-name button.
        }
        bottom {
            buttonbar {
                paddingTop = 20
                button("OK", type = ButtonBar.ButtonData.OK_DONE) {
                    isDefaultButton = true
                    setOnAction {
                        accept = true
                        close()
                    }
                }
                button("Cancel", type = ButtonBar.ButtonData.LEFT) {
                    isCancelButton = true
                    setOnAction {
                        accept = false
                        close()
                    }
                }
                button("Proceed Without") {
                    setOnAction {
                        tableView.clearSelection()
                        accept = true
                        close()
                    }
                }
            }
        }
    }

    fun show(): ProviderSearchResultView? = runBlocking(JavaFx) {
        openModal(block = true)
        if (accept) {
            tableView.selectedItem
        } else {
            null
        }
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