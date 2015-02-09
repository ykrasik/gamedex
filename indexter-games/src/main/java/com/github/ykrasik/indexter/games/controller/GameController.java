package com.github.ykrasik.indexter.games.controller;

import com.github.ykrasik.indexter.exception.ExceptionWrappers;
import com.github.ykrasik.indexter.exception.IndexterException;
import com.github.ykrasik.indexter.games.config.GameCollectionConfig;
import com.github.ykrasik.indexter.games.datamodel.GamePlatform;
import com.github.ykrasik.indexter.games.datamodel.ImageData;
import com.github.ykrasik.indexter.games.datamodel.persistence.ExcludedPath;
import com.github.ykrasik.indexter.games.datamodel.persistence.Game;
import com.github.ykrasik.indexter.games.datamodel.persistence.Genre;
import com.github.ykrasik.indexter.games.datamodel.persistence.Library;
import com.github.ykrasik.indexter.games.manager.exclude.ExcludedPathManager;
import com.github.ykrasik.indexter.games.manager.flow.FlowManager;
import com.github.ykrasik.indexter.games.manager.game.GameManager;
import com.github.ykrasik.indexter.games.manager.game.GameSort;
import com.github.ykrasik.indexter.games.manager.library.LibraryManager;
import com.github.ykrasik.indexter.games.ui.GameInfoCell;
import com.github.ykrasik.indexter.games.ui.SearchableCheckListViewDialog;
import com.github.ykrasik.indexter.optional.Optionals;
import com.github.ykrasik.indexter.util.ListUtils;
import com.google.common.base.Joiner;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.*;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.controlsfx.control.GridView;
import org.controlsfx.control.StatusBar;
import org.controlsfx.dialog.Dialogs;

import java.awt.*;
import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import static com.github.ykrasik.indexter.optional.Optionals.toStringOrUnavailable;

/**
 * @author Yevgeny Krasik
 */
// TODO: Allow changing thumbnail & poster via right-click.
// TODO: Add detail view on double click
// TODO: Add right-click menus to library list.
@Slf4j
@RequiredArgsConstructor
public class GameController {
    private static final Joiner JOINER = Joiner.on(", ").skipNulls();
    public static final Image NOT_AVAILABLE = new Image(GameController.class.getResourceAsStream("/com/github/ykrasik/indexter/games/ui/not_available.png"));

    @FXML private CheckMenuItem showLog;
    @FXML private CheckMenuItem showSideBar;

    @FXML private TextField gameSearch;
    @FXML private Button clearGameSearch;
    @FXML private ComboBox<String> gameSort;

    @FXML private HBox contentScreen;
    @FXML private GridView<Game> gameWall;

    @FXML private TableView<Game> gameList;
    @FXML private TableColumn<Game, String> gameNameColumn;
    @FXML private TableColumn<Game, String> gamePlatformColumn;
    @FXML private TableColumn<Game, String> gameReleaseDateColumn;
    @FXML private TableColumn<Game, Number> gameCriticScoreColumn;
    @FXML private TableColumn<Game, Number> gameUserScoreColumn;
    @FXML private TableColumn<Game, String> gamePathColumn;
    @FXML private TableColumn<Game, String> gameDateAddedColumn;

    // FIXME: Keep each tab in it's own FXML.
    @FXML private VBox sideBar;
    @FXML private ImageView poster;
    @FXML private TextField gamePath;
    @FXML private TextField name;
    @FXML private TextArea description;
    @FXML private TextField platform;
    @FXML private TextField releaseDate;
    @FXML private TextField criticScore;
    @FXML private TextField userScore;
    @FXML private TextField genres;
    @FXML private Hyperlink url;

    @FXML private VBox bottomContainer;
    @FXML private StatusBar statusBar;
    @FXML private Label gameCount;
    @FXML private Label libraryCount;
    @FXML private ToggleButton toggleLog;
    @FXML private ProgressIndicator progressIndicator;
    @FXML private Button statusBarStopButton;
    @FXML private TextArea logTextArea;

    @FXML private TableView<Library> libraryList;
    @FXML private TableColumn<Library, String> libraryName;
    @FXML private TableColumn<Library, String> libraryPlatform;
    @FXML private TableColumn<Library, String> libraryPath;

    @FXML private ListView<ExcludedPath> excludedPathsList;

    @NonNull private final Stage stage;
    @NonNull private final GameCollectionConfig config;
    @NonNull private final FlowManager flowManager;
    @NonNull private final GameManager gameManager;
    @NonNull private final LibraryManager libraryManager;
    @NonNull private final ExcludedPathManager excludedPathManager;

    // Called by JavaFx
    public void initialize() {
        initMenu();
        initGamesTab();
        initLibrariesTab();
        initExcludedTab();
        initBottom();
    }

    private void initMenu() {
        showSideBar.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                contentScreen.getChildren().add(sideBar);
            } else {
                contentScreen.getChildren().remove(sideBar);
            }
        });
    }

    // TODO: This should be it's own fxml.
    private void initGamesTab() {
        initGameWall();
        initGameList();
        initGameSearch();
        initGameSort();
    }

    private void initGameWall() {
        gameWall.setCellFactory(param -> {
            final GameInfoCell cell = new GameInfoCell();
            cell.getStyleClass().add("gameTile");
            cell.setOnMouseClicked(event -> {
                final Game game = cell.getItem();
                displayGameOnSidePanel(game);
                event.consume();
            });

            addGameInfoCellContextMenu(cell);
            return cell;
        });

        // TODO: gameWall has a problem refreshing... so instead of binding, add a listener and clear the wall before setting the value.
        gameWall.itemsProperty().bind(gameManager.gamesProperty());
    }

    private void addGameInfoCellContextMenu(GameInfoCell cell) {
        final ContextMenu contextMenu = new ContextMenu();

        final MenuItem deleteItem = new MenuItem("Delete");
        deleteItem.setOnAction(e -> {
            final Game game = cell.getItem();
            // TODO: Confirmation menu. And go through flowManager.
            gameManager.deleteGame(game);
        });

        contextMenu.getItems().addAll(deleteItem);
        cell.setContextMenu(contextMenu);
    }

    private void initGameList() {
        gameNameColumn.setCellValueFactory(e -> new SimpleStringProperty(e.getValue().getName()));
        gamePlatformColumn.setCellValueFactory(e -> new SimpleStringProperty(e.getValue().getPlatform().toString()));
        gameReleaseDateColumn.setCellValueFactory(e -> new SimpleStringProperty(toStringOrUnavailable(e.getValue().getReleaseDate())));
        gameCriticScoreColumn.setCellValueFactory(e -> new SimpleDoubleProperty(e.getValue().getCriticScore().orElse(0.0)));
        gameUserScoreColumn.setCellValueFactory(e -> new SimpleDoubleProperty(e.getValue().getUserScore().orElse(0.0)));
        gamePathColumn.setCellValueFactory(e -> new SimpleStringProperty(e.getValue().getPath().toString()));
        gameDateAddedColumn.setCellValueFactory(e -> new SimpleStringProperty(e.getValue().getLastModified().toLocalDate().toString()));

        gameList.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Game>() {
            @Override
            public void changed(ObservableValue<? extends Game> observable, Game oldValue, Game newValue) {
                if (newValue != null) {
                    displayGameOnSidePanel(newValue);
                }
            }
        });

        gameList.itemsProperty().bind(gameManager.gamesProperty());
    }

    private void initGameSearch() {
        gameSearch.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                if (newValue.isEmpty()) {
                    gameManager.noNameFilter();
                } else {
                    gameManager.nameFilter(newValue);
                }
            }
        });

        // TODO: This is a bit patchy, could probably use a dedicated class that reads resources.
        final InputStream resource = getClass().getResourceAsStream("/com/github/ykrasik/indexter/games/ui/x_small_icon.png");
        final Image image = new Image(resource);
        clearGameSearch.setGraphic(new ImageView(image));
        clearGameSearch.setOnAction(e -> gameSearch.clear());
    }

    private void initGameSort() {
        gameSort.setItems(FXCollections.observableArrayList(GameSort.getKeys()));
        gameSort.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                final GameSort sort = GameSort.fromString(newValue);
                if (sort == null) {
                    throw new IllegalArgumentException("Invalid sort: " + newValue);
                }
                gameManager.sort(sort);
            }
        });
        gameSort.setValue(GameSort.NAME.getKey());
    }

    // TODO: This should be it's own fxml.
    private void initLibrariesTab() {
        libraryName.setCellValueFactory(e -> new SimpleStringProperty(e.getValue().getName()));
        libraryPlatform.setCellValueFactory(e -> new SimpleStringProperty(e.getValue().getPlatform().toString()));
        libraryPath.setCellValueFactory(e -> new SimpleStringProperty(e.getValue().getPath().toString()));

        libraryList.itemsProperty().bind(libraryManager.librariesProperty());

//        libraryList.setContextMenu(createLibraryContextMenu());
    }

    // TODO: This should be it's own fxml.
    private void initExcludedTab() {
        excludedPathsList.itemsProperty().bind(excludedPathManager.excludedPathsProperty());
    }

    private void initBottom() {
        progressIndicator.progressProperty().bind(flowManager.fetchProgressProperty());
        statusBar.progressProperty().bind(flowManager.progressProperty());
        statusBar.textProperty().bind(flowManager.messageProperty());
        flowManager.messageProperty().addListener((observable, oldValue, newValue) -> {
            logTextArea.appendText(newValue);
            logTextArea.appendText("\n");
        });

        toggleLog.selectedProperty().addListener((observable, oldValue, newValue) -> toggleLogTextArea(newValue));

        gameCount.textProperty().bind(gameManager.gamesProperty().sizeProperty().asString("Games: %d"));
        libraryCount.textProperty().bind(libraryManager.librariesProperty().sizeProperty().asString("Libraries: %d"));
    }

    private void toggleLogTextArea(boolean newValue) {
        if (newValue) {
            bottomContainer.getChildren().add(0, logTextArea);
        } else {
            bottomContainer.getChildren().remove(logTextArea);
        }
    }

    @SneakyThrows
    private void displayGameOnSidePanel(Game game) {
        poster.setImage(Optionals.or(game.getPoster(), game.getThumbnail()).map(ImageData::getImage).orElse(NOT_AVAILABLE));
        gamePath.setText(game.getPath().toString());
        name.setText(game.getName());
        description.setText(toStringOrUnavailable(game.getDescription()));
        platform.setText(game.getPlatform().name());
        releaseDate.setText(toStringOrUnavailable(game.getReleaseDate()));
        criticScore.setText(toStringOrUnavailable(game.getCriticScore()));
        userScore.setText(toStringOrUnavailable(game.getUserScore()));
        genres.setText(JOINER.join(ListUtils.map(game.getGenres(), Genre::getName)));

        url.setText(game.getUrl());
        url.setVisited(false);
        url.setOnAction(e -> ExceptionWrappers.rethrow(() -> Desktop.getDesktop().browse(new URI(game.getUrl()))));
    }

//    private ContextMenu createLibraryContextMenu() {
//        final ContextMenu contextMenu = new ContextMenu();
//
//        final MenuItem deleteItem = new MenuItem("Delete");
//        deleteItem.setOnAction(e -> {
//            final Object source = e.getSource();
//            final EventTarget target = e.getTarget();
////            final LocalGame game = cell.getItem();
////            libraryManager.deleteLibrary(game);
//        });
//
//        contextMenu.getItems().addAll(deleteItem);
//        return contextMenu;
//    }

    @FXML
    // FIXME: Move this into FlowManager.
    public void addLibrary() {
        final DirectoryChooser directoryChooser = createDirectoryChooser("Add Library");
        final File selectedDirectory = directoryChooser.showDialog(stage);
        if (selectedDirectory != null) {
            config.setPrevDirectory(selectedDirectory);
            final Path path = Paths.get(selectedDirectory.toURI());
            try {
                if (libraryManager.isLibrary(path)) {
                    throw new IndexterException("Already have a library defined for '%s'", path);
                }

                // FIXME: Show a select name dialog too.
                final Optional<GamePlatform> platform = createDialog()
                    .title("Choose library platform")
                    .masthead(path.toString())
                    .message("Choose library platform:")
                    .showChoices(GamePlatform.values());
                platform.ifPresent(p -> libraryManager.createLibrary(path.getFileName().toString(), path, p));
            } catch (Exception e) {
                handleException(e);
            }
        }
    }

    @FXML
    public void refreshLibraries() {
        prepareTask(flowManager.refreshLibraries());
    }

    @FXML
    public void cleanupGames() {
        prepareTask(flowManager.cleanupGames());
    }

    @FXML
    public void filterGenres() {
        final Optional<List<Genre>> selectedGenres = new SearchableCheckListViewDialog<Genre>()
            .owner(stage)
            .title("Select Genres:")
            .show(gameManager.getAllGenres());

        selectedGenres.ifPresent(genres -> {
            if (genres.isEmpty()) {
                gameManager.noGenreFilter();
            } else {
                gameManager.genreFilter(genres);
            }
        });
    }

    private void prepareTask(Task<Void> task) {
        task.setOnFailed(event -> handleException(task.getException()));
        task.setOnCancelled(event -> flowManager.stopTask(task));

        progressIndicator.visibleProperty().bind(task.runningProperty());

        statusBarStopButton.disableProperty().bind(task.runningProperty().not());
        statusBarStopButton.visibleProperty().bind(task.runningProperty());
        statusBarStopButton.setOnAction(e -> task.cancel());

        // TODO: Disable all other buttons while task is running.
    }

    private void handleException(Throwable t) {
        log.warn("Error cleaning up games:", t);
        createDialog().title("Error:").message(t.getMessage()).showException(t);
    }

    private DirectoryChooser createDirectoryChooser(String title) {
        final DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle(title);
        directoryChooser.setInitialDirectory(config.getPrevDirectory().orElse(null));
        return directoryChooser;
    }

    private Dialogs createDialog() {
        return Dialogs.create().owner(stage);
    }
}
