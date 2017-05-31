package com.gitlab.ykrasik.gamedex.ui.fragment

import com.gitlab.ykrasik.gamedex.ProviderSearchResult
import com.gitlab.ykrasik.gamedex.Score
import com.gitlab.ykrasik.gamedex.core.ImageLoader
import com.gitlab.ykrasik.gamedex.core.SearchChooser
import com.gitlab.ykrasik.gamedex.repository.GameProviderRepository
import com.gitlab.ykrasik.gamedex.ui.*
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.value.ObservableValue
import javafx.geometry.HPos
import javafx.geometry.Pos
import javafx.geometry.VPos
import javafx.scene.control.TableCell
import javafx.scene.control.TableView
import javafx.scene.image.ImageView
import javafx.scene.layout.Priority
import javafx.scene.paint.Color
import javafx.stage.Screen
import org.controlsfx.glyphfont.FontAwesome
import tornadofx.*
import java.awt.Desktop
import java.util.*

/**
 * User: ykrasik
 * Date: 02/01/2017
 * Time: 15:54
 */
// TODO: If the game contains an .nfo file, consider displaying it in the search window
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
        with(Screen.getPrimary().bounds) {
            prefHeight = height * 3 / 4
            prefWidth = width * 2 / 3
        }
        center {
            borderpane {
                paddingAll = 20
                top {
                    gridpane {
                        hgap = 10.0
                        jfxButton(data.path.path) {
                            gridpaneConstraints { columnRowIndex(0, 0); vAlignment = VPos.TOP; hAlignment = HPos.LEFT }
                            setId(Style.pathLabel)
                            isFocusTraversable = false
                            setOnAction { Desktop.getDesktop().open(data.path) }
                        }
                        region { gridpaneConstraints { columnRowIndex(1, 0); hGrow = Priority.ALWAYS } }
                        stackpane {
                            gridpaneConstraints { columnRowIndex(4, 0); vAlignment = VPos.TOP; hAlignment = HPos.RIGHT }
                            children += data.platform.toLogo { size(40.0) }
                        }
                        imageview(providerRepository.logo(data.providerId)) {
                            gridpaneConstraints { columnRowIndex(5, 0); vAlignment = VPos.TOP; hAlignment = HPos.RIGHT }
                            fitHeight = 80.0
                            fitWidth = 160.0
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
                            gridpaneConstraints { columnRowIndex(5, 1); vAlignment = VPos.BOTTOM; hAlignment = HPos.RIGHT }
                            setId(Style.searchResultsLabel)
                            textProperty().bind(results.mapProperty { "Search results: ${results.size}" })
                        }
                        paddingBottom = 10
                    }
                }

                center {
                    tableView = tableview(results) {
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

                        onUserSelect(clickCount = 2) { close(okResult) }
                    }
                }
            }
        }
        top {
            toolbar {
                acceptButton {
                    enableWhen { tableView.selectionModel.selectedItemProperty().isNotNull }
//                    defaultButtonProperty().bind(defaultButtonIsSearch.not())
                    setOnAction { close(okResult) }
                }
                verticalSeparator()
                jfxButton("Not Exact Match", graphic = FontAwesome.Glyph.QUESTION.toGraphic{ size(26.0); color(Color.LIGHTGREEN) }) {
                    setId(Style.notExactMatch)
                    addClass(CommonStyle.toolbarButton)
                    tooltip("Not Exact Match")
                    enableWhen { tableView.selectionModel.selectedItemProperty().isNotNull }
                    setOnAction { close(SearchChooser.Choice.NotExactMatch(tableView.selectedItem!!)) }
                }
                verticalSeparator()
                backButton("Proceed Without") {
                    setId(Style.proceedWithout)
                    tooltip("Proceed Without")
                    setOnAction { close(SearchChooser.Choice.ProceedWithout) }
                }
                verticalSeparator()
                spacer()
                verticalSeparator()
                jfxButton("Exclude ${data.providerId}", graphic = FontAwesome.Glyph.EXCLAMATION_TRIANGLE.toGraphic{ size(26.0) }) {
                    setId(Style.excludeProvider)
                    addClass(CommonStyle.toolbarButton)
                    tooltip("Exclude searching ${data.providerId} for '${data.name}'")
                    setOnAction { close(SearchChooser.Choice.ExcludeProvider(data.providerId))  }
                }
                verticalSeparator()
                cancelButton { setOnAction { close(SearchChooser.Choice.Cancel)  } }
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

    private fun toScoreDisplay(score: Score?, type: String): ObservableValue<String> =
        (if (score == null) "" else "${score.score}   |   ${score.numReviews} $type").toProperty()

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
            val notExactMatch by cssid()
            val proceedWithout by cssid()
            val excludeProvider by cssid()
            val centered by cssclass()
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
            notExactMatch {
                and(hover) {
                    backgroundColor = multi(Color.LIGHTYELLOW)
                }
            }
            proceedWithout {
                and(hover) {
                    backgroundColor = multi(Color.YELLOW)
                }
            }
            excludeProvider {
                and(hover) {
                    backgroundColor = multi(Color.ORANGE)
                }
            }
            centered {
                alignment = Pos.TOP_CENTER
            }
        }
    }
}