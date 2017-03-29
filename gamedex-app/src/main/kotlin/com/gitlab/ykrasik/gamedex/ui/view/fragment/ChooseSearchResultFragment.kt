package com.gitlab.ykrasik.gamedex.ui.view.fragment

import com.gitlab.ykrasik.gamedex.provider.DataProviderInfo
import com.gitlab.ykrasik.gamedex.provider.ProviderSearchResult
import com.gitlab.ykrasik.gamedex.provider.SearchResultChoice
import com.gitlab.ykrasik.gamedex.ui.cancelButton
import com.gitlab.ykrasik.gamedex.ui.customColumn
import com.gitlab.ykrasik.gamedex.ui.fadeOnImageChange
import com.gitlab.ykrasik.gamedex.ui.okButton
import javafx.beans.property.DoubleProperty
import javafx.beans.property.ReadOnlyObjectProperty
import javafx.beans.property.SimpleDoubleProperty
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
import java.io.File

/**
 * User: ykrasik
 * Date: 02/01/2017
 * Time: 15:54
 */
class ChooseSearchResultFragment(
    private val searchedName: String,
    private val path: File,
    private val info: DataProviderInfo,
    private val searchResults: ObservableList<ProviderSearchResultWithThumbnail>,
    private val canProceedWithout: Boolean
) : Fragment("Choose Search Result for '$searchedName'") {
    private var tableView: TableView<ProviderSearchResultWithThumbnail> by singleAssign()
    private val minTableWidth: DoubleProperty = SimpleDoubleProperty()

    private var choice: SearchResultChoice = SearchResultChoice.Cancel

    override val root = borderpane {
        paddingAll = 20
        top {
            gridpane {
                row {
                    label(path.path) {
                        gridpaneConstraints { vAlignment = VPos.TOP; hAlignment = HPos.LEFT }
                        font = Font.font(14.0)
                    }
                    region { gridpaneConstraints { hGrow = Priority.ALWAYS } }
                    imageview {
                        gridpaneConstraints { vAlignment = VPos.TOP; hAlignment = HPos.RIGHT }
                        image = info.logo
                        fitHeight = 100.0
                        fitWidth = 100.0
                        isPreserveRatio = true
                    }
                }
                row {
                    gridpane {
                        hgap = 10.0
                        gridpaneConstraints { vAlignment = VPos.BOTTOM; hAlignment = HPos.LEFT }
                        row {
                            val newSearch = textfield(searchedName) {
                                prefWidthProperty().bind(minTableWidth.subtract(230))
                                tooltip("You can edit the name and click 'Search Again' to search for a new value")
                                font = Font.font(16.0)
                                isFocusTraversable = false
                            }
                            button("Search Again") {
                                prefHeightProperty().bind(newSearch.heightProperty())
                                tooltip("Search for a new name, enter new name in the name field")
                                setOnAction { close(choice = SearchResultChoice.NewSearch(newSearch.text)) }
                            }
                        }
                    }
                    region { gridpaneConstraints { hGrow = Priority.ALWAYS } }
                    label("Search results: ${searchResults.size}") {
                        gridpaneConstraints { vAlignment = VPos.BOTTOM; hAlignment = HPos.RIGHT }
                        font = Font.font(16.0)
                    }
                }
                paddingBottom = 10
            }
        }
        center {
            tableView = tableview(searchResults) {
                val indexColumn = makeIndexColumn()
                customColumn("Thumbnail", ProviderSearchResultWithThumbnail::thumbnail) {
                    object : TableCell<ProviderSearchResultWithThumbnail, ReadOnlyObjectProperty<Image>>() {
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
                column("Name", ProviderSearchResultWithThumbnail::name)
                column("Release Date", ProviderSearchResultWithThumbnail::releaseDate)
                column("Score", ProviderSearchResultWithThumbnail::score)

                minTableWidth.bind(contentColumns.fold(indexColumn.widthProperty().subtract(10)) { binding, column ->
                    binding.add(column.widthProperty())
                })
                minWidthProperty().bind(minTableWidth)

                onUserSelect(clickCount = 2) { close(choice = okResult) }
            }
            // FIXME: Add a search-again-with-new-name button.
        }
        bottom {
            buttonbar {
                paddingTop = 20
                okButton { 
                    enableWhen { tableView.selectionModel.selectedItemProperty().isNotNull }
                    setOnAction { close(choice = okResult) }
                }
                cancelButton { setOnAction { close(choice = SearchResultChoice.Cancel) } }
                if (canProceedWithout) {
                    button("Proceed Without") { setOnAction { close(choice = SearchResultChoice.ProceedWithout) } }
                }
            }
        }
    }

    override fun onDock() {
        tableView.resizeColumnsToFitContent()
        modalStage!!.minWidthProperty().bind(minTableWidth.add(60))
    }

    suspend fun show(): SearchResultChoice = run(JavaFx) {
        openModal(block = true)
        choice
    }

    private fun close(choice: SearchResultChoice) {
        this.choice = choice
        close()
    }

    private val okResult get() = SearchResultChoice.Ok(tableView.selectedItem!!.searchResult)
}

class ProviderSearchResultWithThumbnail(
    val searchResult: ProviderSearchResult,
    val thumbnail: ReadOnlyObjectProperty<Image>
) {
    val name get() = searchResult.name
    val releaseDate get() = searchResult.releaseDate
    val score get() = searchResult.score

    override fun toString() = searchResult.toString()
}