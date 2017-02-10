package com.gitlab.ykrasik.gamedex.ui.view.fragment

import com.gitlab.ykrasik.gamedex.common.util.toImage
import com.gitlab.ykrasik.gamedex.core.ImageLoader
import com.gitlab.ykrasik.gamedex.provider.DataProviderInfo
import com.gitlab.ykrasik.gamedex.provider.ProviderSearchResult
import com.gitlab.ykrasik.gamedex.provider.SearchContext
import javafx.collections.ObservableList
import javafx.scene.control.ButtonBar
import javafx.scene.control.TableCell
import javafx.scene.control.TableView
import javafx.scene.image.ImageView
import tornadofx.*
import kotlin.collections.set

/**
 * User: ykrasik
 * Date: 02/01/2017
 * Time: 15:54
 */
class ChooseSearchResultFragment(
    private val context: SearchContext,
    private val info: DataProviderInfo,
    private val providerSearchResults: ObservableList<ProviderSearchResult>
) : Fragment("Choose Search Result") {
    private val imageLoader: ImageLoader by di()

    private var tableView: TableView<ProviderSearchResult> by singleAssign()
    private var accept = false

    override val root = borderpane {
        top {
            vbox {
                label(context.path.path)
                imageview {
                    image = info.logo
                    fitHeight = 200.0
                    fitWidth = 500.0
                    isPickOnBounds = true   // TODO: What is this?
                    isPreserveRatio = true
                }
                label("This displays errors")
                label("Results: ${providerSearchResults.size}")
            }
        }
        center {
            tableView = tableview(providerSearchResults) {
                makeIndexColumn()
                column("thumbnail", ProviderSearchResult::thumbnailUrl) {
                    setCellFactory {
                        object : TableCell<ProviderSearchResult, String?>() {
                            private val imageView = ImageView()
                            override fun updateItem(thumbnailUrl: String?, empty: Boolean) {
                                if (empty) {
                                    graphic = null
                                } else {
                                    thumbnailUrl?.let { url ->
                                        graphic = imageView
                                        val thumbnail = context.thumbnailCache[url]
                                        if (thumbnail != null) {
                                            imageView.image = thumbnail.toImage()
                                        } else {
                                            imageLoader.loadUrl(url, imageView).onSucceeded {
                                                context.thumbnailCache[url] = it
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                column("Name", ProviderSearchResult::name)
                column("Release Date", ProviderSearchResult::releaseDate)
                column("Score", ProviderSearchResult::score)

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
                button("Cancel", type = ButtonBar.ButtonData.CANCEL_CLOSE) {
                    isCancelButton = true
                    setOnAction {
                        accept = false
                        closeModal()
                    }
                }
                button("Proceed Anyway") {
                    // TODO: Figure this one out.
                }
                button("OK", type = ButtonBar.ButtonData.OK_DONE) {
                    isDefaultButton = true
                    setOnAction {
                        accept = true
                        closeModal()
                    }
                }
            }
        }
    }

    fun show(): ProviderSearchResult? {
        openModal(block = true)
        return if (accept) {
            tableView.selectedItem
        } else {
            null
        }
    }
}

//class GameSearchScreen
//@SneakyThrows
//constructor(@NonNull private val stageManager: StageManager) {
//    @FXML private val pathLabel: Label? = null
//    @FXML private val logoImageView: ImageView? = null
//    @FXML private val errorLabel: Label? = null
//
//    @FXML private val searchTextField: TextField? = null
//    @FXML private val searchButton: Button? = null
//    @FXML private val searchResultsCountLabel: Label? = null
//
//    @FXML private val multipleResultsCheckBox: CheckBox? = null
//    @FXML private val autoContinueCheckBox: CheckBox? = null
//
//    @FXML private val searchResultsTable: TableView<SearchResult>? = null
//    @FXML private val checkColumn: TableColumn<SearchResult, Boolean>? = null
//    @FXML private val nameColumn: TableColumn<SearchResult, String>? = null
//    @FXML private val scoreColumn: TableColumn<SearchResult, String>? = null
//    @FXML private val releaseDateColumn: TableColumn<SearchResult, String>? = null
//
//    @FXML private val proceedAnywayButton: Button? = null
//    @FXML private val okButton: Button? = null
//
//    private val stage = Stage()
//    private val searchResultsProperty = SimpleListProperty(FXCollections.emptyObservableList<SearchResult>())
//
//    private var result: GameSearchChoice? = null
//
//    init {
//
//        val loader = FXMLLoader(UIResources.gameSearchScreenFxml())
//        loader.setController(this)
//        val root = loader.load()
//
//        val scene = Scene(root, Color.TRANSPARENT)
//        scene.stylesheets.addAll(UIResources.mainCss(), UIResources.gameSearchScreenCss())
//
//        stage.initStyle(StageStyle.UNDECORATED)
//        stage.initModality(Modality.APPLICATION_MODAL)
//        stage.scene = scene
//
//        // Make the stage draggable by clicking anywhere.
//        JavaFxUtils.makeStageDraggable(stage, root)
//    }
//
//    @FXML
//    private fun initialize() {
//        initSearchResultsTable()
//
//        searchResultsCountLabel!!.textProperty().bind(searchResultsProperty.sizeProperty().asString("Results: %d"))
//
//        searchButton!!.defaultButtonProperty().bind(searchTextField!!.focusedProperty())
//        okButton!!.defaultButtonProperty().bind(searchButton.defaultButtonProperty().not())
//        okButton.disableProperty().bind(searchResultsTable!!.selectionModel.selectedItemProperty().isNull)
//        okButton.setOnAction { e -> setResultFromSelection() }
//    }
//
//    private fun initSearchResultsTable() {
//        checkColumn!!.setCellFactory { param -> CheckBoxTableCell<SearchResult, Boolean>() }
//        nameColumn!!.setCellValueFactory { e -> SimpleStringProperty(e.value.name) }
//        releaseDateColumn!!.setCellValueFactory { e -> SimpleStringProperty(toStringOrUnavailable(e.value.releaseDate)) }
//        scoreColumn!!.setCellValueFactory { e -> SimpleStringProperty(toStringOrUnavailable(e.value.score)) }
//
//        searchResultsTable!!.itemsProperty().bind(searchResultsProperty)
//        searchResultsTable.setOnMouseClicked { e ->
//            if (e.clickCount == 2) {
//                setResultFromSelection()
//            }
//        }
//
//        searchButton!!.setOnAction { e ->
//            val newName = searchTextField!!.text
//            setResult(GameSearchChoice.newName(newName))
//        }
//    }
//
//    fun show(searchedName: String, path: Path, info: DataProviderInfo, searchResults: ImmutableList<SearchResult>): GameSearchChoice {
//        logoImageView!!.setImage(info.logo)
//        pathLabel!!.text = path.toString()
//        searchTextField!!.text = searchedName
//        errorLabel!!.text = getErrorLabel(searchedName, searchResults)
//        setSearchResults(searchResults)
//        proceedAnywayButton!!.isDisable = info.isRequired()
//
//        stageManager.runWithBlur(RunnableX { stage.showAndWait() })
//        return result
//    }
//
//    private fun getErrorLabel(searchedName: String, searchResults: ImmutableList<SearchResult>): String {
//        if (searchResults.isEmpty()) {
//            return String.format("No search results for '%s'!", searchedName)
//        } else {
//            return String.format("Too many search results for '%s'!", searchedName)
//        }
//    }
//
//    private fun setSearchResults(searchResults: ImmutableList<SearchResult>) {
//        searchResultsProperty.set(FXCollections.observableArrayList<SearchResult>(searchResults.castToList()))
//    }
//
//    private fun setResultFromSelection() {
//        val selectedItem = searchResultsTable!!.selectionModel.selectedItem
//        if (selectedItem != null) {
//            setResult(GameSearchChoice.select(selectedItem))
//        }
//    }
//
//    @FXML
//    private fun skip() {
//        setResult(GameSearchChoice.skip())
//    }
//
//    @FXML
//    private fun exclude() {
//        setResult(GameSearchChoice.exclude())
//    }
//
//    @FXML
//    private fun proceedAnyway() {
//        setResult(GameSearchChoice.proceedAnyway())
//    }
//
//    private fun setResult(choice: GameSearchChoice) {
//        this.result = choice
//        stage.hide()
//    }
//}