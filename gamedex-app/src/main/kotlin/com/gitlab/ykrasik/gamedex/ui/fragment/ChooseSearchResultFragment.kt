package com.gitlab.ykrasik.gamedex.ui.fragment

import com.gitlab.ykrasik.gamedex.ProviderSearchResult
import com.gitlab.ykrasik.gamedex.core.ImageLoader
import com.gitlab.ykrasik.gamedex.core.SearchChooser
import com.gitlab.ykrasik.gamedex.repository.GameProviderRepository
import com.gitlab.ykrasik.gamedex.ui.*
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleDoubleProperty
import javafx.geometry.HPos
import javafx.geometry.Pos
import javafx.geometry.VPos
import javafx.scene.control.ButtonBar
import javafx.scene.control.ContentDisplay
import javafx.scene.control.TableCell
import javafx.scene.control.TableView
import javafx.scene.image.ImageView
import javafx.scene.layout.Priority
import javafx.stage.Screen
import org.controlsfx.glyphfont.FontAwesome
import tornadofx.*

/**
 * User: ykrasik
 * Date: 02/01/2017
 * Time: 15:54
 */
class ChooseSearchResultFragment(data: SearchChooser.Data) : Fragment("Choose Search Result for '${data.name}'") {
    private val imageLoader: ImageLoader by di()
    private val providerRepository: GameProviderRepository by di()

    private val minTableWidth = SimpleDoubleProperty()
    private val defaultButtonIsSearch = SimpleBooleanProperty(false)

    private var tableView: TableView<ProviderSearchResult> by singleAssign()

    private val results = ArrayList(data.results).observable()

    private val showingFilteredProperty = SimpleBooleanProperty(false)
    private var showingFiltered by showingFilteredProperty

    private var choice: SearchChooser.Choice = SearchChooser.Choice.Cancel

    override val root = borderpane {
        paddingAll = 20
        with(Screen.getPrimary().bounds) {
            prefHeight = height * 3 / 4
            prefWidth = width * 2 / 3
        }
        top {
            gridpane {
                hgap = 10.0
                label(data.path.path) {
                    gridpaneConstraints { columnRowIndex(0, 0); vAlignment = VPos.TOP; hAlignment = HPos.LEFT }
                    setId(Style.pathLabel)
                }
                region { gridpaneConstraints { columnRowIndex(1, 0); hGrow = Priority.ALWAYS } }
                imageview {
                    gridpaneConstraints { columnRowIndex(4, 0); vAlignment = VPos.TOP; hAlignment = HPos.RIGHT }
                    image = providerRepository.logo(data.providerType)
                    fitHeight = 100.0
                    fitWidth = 100.0
                    isPreserveRatio = true
                }

                val newSearch = textfield(data.name) {
                    gridpaneConstraints { columnRowIndex(0, 1) }
                    setId(Style.newSearch)
                    prefWidthProperty().bind(minTableWidth.subtract(230))
                    tooltip("Search for a different name")
                    isFocusTraversable = false

                    // "Search Again" becomes the new default button when this textfield has focus.
                    defaultButtonIsSearch.bind(focusedProperty())
                }
                button("Search Again") {
                    gridpaneConstraints { columnRowIndex(1, 1) }
                    enableWhen { newSearch.textProperty().isNotEqualTo(data.name) }
                    defaultButtonProperty().bind(defaultButtonIsSearch)
                    prefHeightProperty().bind(newSearch.heightProperty())
                    tooltip("Search for a different name")
                    setOnAction { close(SearchChooser.Choice.NewSearch(newSearch.text)) }
                }
                region { gridpaneConstraints { columnRowIndex(2, 1); hGrow = Priority.ALWAYS } }

                if (data.filteredResults.isNotEmpty()) {
                    jfxButton {
                        gridpaneConstraints { columnRowIndex(3, 1); vAlignment = VPos.BOTTOM; hAlignment = HPos.RIGHT }
                        setId(Style.showFilterToggle)
                        graphicProperty().bind(showingFilteredProperty.map {
                            (if (it!!) FontAwesome.Glyph.MINUS else FontAwesome.Glyph.PLUS).toGraphic()
                        })
                        tooltip {
                            textProperty().bind(
                                showingFilteredProperty.map {
                                    "${if (it!!) "Hide" else "Show"} ${data.filteredResults.size} filtered results"
                                }
                            )
                        }
                        setOnAction {
                            showingFiltered = !showingFiltered
                        }
                    }
                }
                label {
                    gridpaneConstraints { columnRowIndex(4, 1); vAlignment = VPos.BOTTOM; hAlignment = HPos.RIGHT }
                    setId(Style.searchResultsLabel)
                    textProperty().bind(results.mapProperty { "Search results: ${results.size}" })
                }
                paddingBottom = 10
            }
        }
        center {
            tableView = tableview(results) {
                val indexColumn = makeIndexColumn()
                customColumn("Thumbnail", ProviderSearchResult::thumbnailUrl) {
                    object : TableCell<ProviderSearchResult, String?>() {
                        private val imageView = ImageView().apply {
                            fadeOnImageChange()
                            fitHeight = 200.0
                            fitWidth = 200.0
                            isPreserveRatio = true
                        }

                        init {
                            graphic = imageView
                        }

                        override fun updateItem(thumbnailUrl: String?, empty: Boolean) {
                            if (empty) {
                                imageView.imageProperty().unbind()
                            } else {
                                val thumbnail = imageLoader.downloadImage(thumbnailUrl)
                                imageView.imageProperty().cleanBind(thumbnail)
                            }
                        }
                    }
                }
                column("Name", ProviderSearchResult::name)
                column("Release Date", ProviderSearchResult::releaseDate)
                column("Score", ProviderSearchResult::score)

                minTableWidth.bind(contentColumns.fold(indexColumn.widthProperty().subtract(10)) { binding, column ->
                    binding.add(column.widthProperty())
                })
                minWidthProperty().bind(minTableWidth)

                onUserSelect(clickCount = 2) { close(okResult) }
            }
        }
        bottom {
            buttonbar {
                paddingTop = 20
                button("OK", ButtonBar.ButtonData.OK_DONE) {
                    graphic = FontAwesome.Glyph.CARET_DOWN.toGraphic()
                    contentDisplay = ContentDisplay.RIGHT
                    alignment = Pos.CENTER_RIGHT
                    graphicTextGap = 30.0
                    enableWhen { tableView.selectionModel.selectedItemProperty().isNotNull }
                    defaultButtonProperty().bind(defaultButtonIsSearch.not())

                    setOnAction { close(okResult) }
                    dropDownMenu {
                        prefWidthProperty().bind(this@button.widthProperty())
                        menuitem("Not Exact Match") {
                            close(SearchChooser.Choice.NotExactMatch(tableView.selectedItem!!))
                        }
                    }
                }

                button("Proceed Without", type = ButtonBar.ButtonData.FINISH) {
                    setOnAction { close(SearchChooser.Choice.ProceedWithout) }
                }

                cancelButton { setOnAction { close(SearchChooser.Choice.Cancel) } }
            }
        }
    }

    init {
        showingFilteredProperty.onChange {
            if (it) {
                results += data.filteredResults
            } else {
                results -= data.filteredResults
            }
            tableView.resizeColumnsToFitContent()
        }
        if (results.isEmpty() && data.filteredResults.isNotEmpty()) {
            showingFiltered = true
        }
    }

    override fun onDock() {
//        SmartResize.POLICY.requestResize(tableView)   // TODO: Experiment with this.
        tableView.resizeColumnsToFitContent()
        modalStage!!.minWidthProperty().bind(minTableWidth.add(60))
    }

    fun show(): SearchChooser.Choice {
        openWindow(block = true)
        return choice
    }

    private fun close(choice: SearchChooser.Choice) {
        this.choice = choice
        close()
    }

    private val okResult get() = SearchChooser.Choice.ExactMatch(tableView.selectedItem!!)

    class Style : Stylesheet() {
        companion object {
            val pathLabel by cssid()
            val newSearch by cssid()
            val showFilterToggle by cssid()
            val searchResultsLabel by cssid()
        }

        init {
            pathLabel {
                fontSize = 14.px
            }
            newSearch {
                fontSize = 16.px
            }
            showFilterToggle {
                fontSize = 16.px
            }
            searchResultsLabel {
                fontSize = 16.px
            }
        }
    }
}