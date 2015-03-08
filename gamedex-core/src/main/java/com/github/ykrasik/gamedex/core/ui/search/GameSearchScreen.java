package com.github.ykrasik.gamedex.core.ui.search;

import com.github.ykrasik.gamedex.core.javafx.JavaFxUtils;
import com.github.ykrasik.gamedex.core.manager.info.SearchContext;
import com.github.ykrasik.gamedex.core.service.task.TaskService;
import com.github.ykrasik.gamedex.core.ui.UIResources;
import com.github.ykrasik.gamedex.datamodel.provider.SearchResult;
import com.github.ykrasik.gamedex.provider.GameInfoProvider;
import com.github.ykrasik.opt.Opt;
import com.gs.collections.api.list.ImmutableList;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import lombok.NonNull;
import lombok.SneakyThrows;

import static com.github.ykrasik.gamedex.common.util.StringUtils.toStringOrUnavailable;

/**
 * @author Yevgeny Krasik
 */
public class GameSearchScreen {
    @FXML private Label pathLabel;
    @FXML private ImageView logoImageView;

    @FXML private TextField searchTextField;
    @FXML private Button searchButton;
    @FXML private Label searchResultsCountLabel;

    @FXML private CheckBox multipleResultsCheckBox;
    @FXML private CheckBox autoContinueCheckBox;

    @FXML private TableView<SearchResult> searchResultsTable;
    @FXML private TableColumn<SearchResult, Boolean> checkColumn;
    @FXML private TableColumn<SearchResult, String> nameColumn;
    @FXML private TableColumn<SearchResult, String> scoreColumn;
    @FXML private TableColumn<SearchResult, String> releaseDateColumn;

    @FXML private ImageView searchingImageView;

    @FXML private Button proceedAnywayButton;
    @FXML private Button okButton;

    private final Stage stage = new Stage();
    private final ListProperty<SearchResult> searchResultsProperty = new SimpleListProperty<>(FXCollections.emptyObservableList());
    private final BooleanProperty searchingProperty = new SimpleBooleanProperty(false);

    private final TaskService taskService;

    private SearchContext context;
    private GameSearchChoice result;

    @SneakyThrows
    public GameSearchScreen(@NonNull TaskService taskService) {
        this.taskService = taskService;

        final FXMLLoader loader = new FXMLLoader(UIResources.gameSearchScreenFxml());
        loader.setController(this);
        final BorderPane root = loader.load();

        final Scene scene = new Scene(root, Color.TRANSPARENT);
        scene.getStylesheets().addAll(UIResources.mainCss(), UIResources.gameSearchScreenCss());

        stage.initStyle(StageStyle.UNDECORATED);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setScene(scene);

        // Make the stage draggable by clicking anywhere.
        JavaFxUtils.makeDraggable(stage, root);
    }

    @FXML
    private void initialize() {
        initSearchResultsTable();

        searchResultsCountLabel.textProperty().bind(searchResultsProperty.sizeProperty().asString("Results: %d"));

        searchButton.defaultButtonProperty().bind(searchTextField.focusedProperty());
        okButton.defaultButtonProperty().bind(searchButton.defaultButtonProperty().not());
        okButton.disableProperty().bind(searchResultsTable.getSelectionModel().selectedItemProperty().isNull());
        okButton.setOnAction(e -> setResultFromSelection());

        searchingImageView.setImage(UIResources.imageLoading());
        searchingImageView.fitHeightProperty().bind(searchResultsTable.heightProperty().subtract(20));
        searchingImageView.fitWidthProperty().bind(searchResultsTable.widthProperty().subtract(20));
        searchingImageView.visibleProperty().bind(searchingProperty);
        searchResultsTable.disableProperty().bind(searchingProperty);
    }

    private void initSearchResultsTable() {
        checkColumn.setCellFactory(param -> new CheckBoxTableCell<>());
        nameColumn.setCellValueFactory(e -> new SimpleStringProperty(e.getValue().getName()));
        releaseDateColumn.setCellValueFactory(e -> new SimpleStringProperty(toStringOrUnavailable(e.getValue().getReleaseDate())));
        scoreColumn.setCellValueFactory(e -> new SimpleStringProperty(toStringOrUnavailable(e.getValue().getScore())));

        searchResultsTable.itemsProperty().bind(searchResultsProperty);
        searchResultsTable.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                setResultFromSelection();
            }
        });
    }

//    private ImmutableList<SearchResult> search(String name, SearchContext context) throws Exception {
//        message("Searching '%s'...", name);
//        fetchingProperty.set(true);
//        final ImmutableList<SearchResult> searchResults;
//        try {
//            searchResults = gameInfoProvider.search(name, context.platform());
//        } finally {
//            fetchingProperty.set(false);
//        }
//        message("Found %d results for '%s'.", searchResults.size(), name);
//
//        final Collection<String> excludedNames = context.getExcludedNames();
//        if (searchResults.size() <= 1 || excludedNames.isEmpty()) {
//            return searchResults;
//        }
//
//        message("Filtering previously encountered search results...");
//        final ImmutableList<SearchResult> filteredSearchResults = searchResults.select(result -> !excludedNames.contains(result.getName()));
//        if (!filteredSearchResults.isEmpty()) {
//            message("%d remaining results.", filteredSearchResults.size());
//            return filteredSearchResults;
//        } else {
//            message("No search results after filtering, reverting...");
//            return searchResults;
//        }
//    }
//
//    private Opt<GameInfo> handleNoSearchResults(String name, Path path, GamePlatform platform, SearchContext context) throws Exception {
//        assertNotAutoSkip();
//
//        final NoSearchResultsDialogParams params = NoSearchResultsDialogParams.builder()
//            .providerName(gameInfoProvider.getProviderType().getName())
//            .name(name)
//            .path(path)
//            .platform(platform)
//            .canProceedWithout(canProceedWithout)
//            .build();
//        final DialogChoice choice = dialogService.noSearchResultsDialog(params);
//        return choice.resolve(new DefaultDialogChoiceResolver() {
//            @Override
//            public Opt<GameInfo> newName(String newName) throws Exception {
//                return fetchGameInfo(newName, path, platform, context);
//            }
//        });
//    }
//
//    private Opt<GameInfo> handleMultipleSearchResults(String name,
//                                                      Path path,
//                                                      GamePlatform platform,
//                                                      SearchContext context,
//                                                      ImmutableList<SearchResult> searchResults) throws Exception {
//        assertNotAutoSkip();
//
//        final MultipleSearchResultsDialogParams params = MultipleSearchResultsDialogParams.builder()
//            .providerName(gameInfoProvider.getProviderType().getName())
//            .name(name)
//            .path(path)
//            .platform(platform)
//            .searchResults(searchResults)
//            .canProceedWithout(canProceedWithout)
//            .build();
//        final DialogChoice choice = dialogService.multipleSearchResultsDialog(params);
//        return choice.resolve(new DefaultDialogChoiceResolver() {
//            @Override
//            public Opt<GameInfo> newName(String newName) throws Exception {
//                // Add all current search results to excluded list.
//                final ImmutableList<String> searchResultNames = getSearchResultNames(searchResults);
//                context.addExcludedNames(searchResultNames);
//                return fetchGameInfo(newName, path, platform, context);
//            }
//
//            @Override
//            public Opt<GameInfo> choose(SearchResult chosenSearchResult) throws Exception {
//                // Add all other search results to excluded list.
//                final ImmutableList<String> searchResultNames = getSearchResultNames(searchResults);
//                final ImmutableList<String> excludedNames = searchResultNames.newWithout(chosenSearchResult.getName());
//                context.addExcludedNames(excludedNames);
//                return Opt.of(fetchGameInfoFromSearchResult(chosenSearchResult));
//            }
//        });
//    }

    public GameSearchChoice show(GameInfoProvider gameInfoProvider,
                                 String searchedName,
                                 SearchContext context,
                                 ImmutableList<SearchResult> searchResults) {
        this.context = context;

        proceedAnywayButton.setDisable(gameInfoProvider.isRequired());
        proceedAnywayButton.setVisible(gameInfoProvider.isRequired());
        searchButton.setOnAction(e -> searchFromInput(gameInfoProvider));

        pathLabel.setText(context.path().toString());
        searchTextField.setText(searchedName);
        setSearchResults(searchResults);
        stage.showAndWait();
        return result;
    }

    @SneakyThrows
    private void searchFromInput(GameInfoProvider gameInfoProvider) {
        final String name = searchTextField.getText();

        searchingProperty.set(true);
        final Task<ImmutableList<SearchResult>> task = taskService.submit(() -> gameInfoProvider.search(name, context.platform()));
        task.setOnSucceeded(e -> {
            searchingProperty.set(false);

            final ImmutableList<SearchResult> searchResults = task.getValue();
            setSearchResults(searchResults);

            // TODO: If only 1 result and checked to continue, do it.
            if (searchResults.size() == 1) {
                setResult(searchResults.get(0));
            }
        });
    }

    private void setSearchResults(ImmutableList<SearchResult> searchResults) {
        searchResultsProperty.set(FXCollections.observableArrayList(searchResults.castToList()));
    }

    private void setNoResult(GameSearchChoiceType type) {
        if (type == GameSearchChoiceType.OK) {
            throw new IllegalArgumentException("Illegal no result type: " + type);
        }
        result = new GameSearchChoice(Opt.absent(), type);
        stage.hide();
    }

    private void setResultFromSelection() {
        final SearchResult selectedItem = searchResultsTable.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            setResult(selectedItem);
        }
    }

    private void setResult(SearchResult searchResult) {
        result = new GameSearchChoice(Opt.of(searchResult), GameSearchChoiceType.OK);
        stage.hide();
    }

    @FXML
    private void skip() {
        setNoResult(GameSearchChoiceType.SKIP);
    }

    @FXML
    private void exclude() {
        setNoResult(GameSearchChoiceType.EXCLUDE);
    }
}
