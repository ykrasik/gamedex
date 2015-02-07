package com.github.ykrasik.indexter.games.controller;

import com.github.ykrasik.indexter.exception.IndexterException;
import com.github.ykrasik.indexter.games.config.GameCollectionConfig;
import com.github.ykrasik.indexter.games.datamodel.GamePlatform;
import com.github.ykrasik.indexter.games.datamodel.ImageData;
import com.github.ykrasik.indexter.games.datamodel.persistence.Game;
import com.github.ykrasik.indexter.games.datamodel.persistence.Genre;
import com.github.ykrasik.indexter.games.datamodel.persistence.Library;
import com.github.ykrasik.indexter.games.manager.flow.FlowManager;
import com.github.ykrasik.indexter.games.manager.game.GameManager;
import com.github.ykrasik.indexter.games.manager.game.GameSort;
import com.github.ykrasik.indexter.games.manager.library.LibraryManager;
import com.github.ykrasik.indexter.games.ui.GameInfoCell;
import com.github.ykrasik.indexter.games.ui.SearchableCheckListViewDialog;
import com.github.ykrasik.indexter.optional.Optionals;
import com.github.ykrasik.indexter.util.ListUtils;
import com.github.ykrasik.indexter.util.PlatformUtils;
import com.google.common.base.Joiner;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import lombok.NonNull;
import org.controlsfx.control.GridView;
import org.controlsfx.control.StatusBar;
import org.controlsfx.dialog.Dialogs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static com.github.ykrasik.indexter.optional.Optionals.toStringOrUnavailable;

/**
 * @author Yevgeny Krasik
 */
// TODO: Display total number of games somewhere.
// TODO: Allow changing thumbnail & poster via right-click.
// TODO: Add detail view on double click
// TODO: Add right-click menus to library list.
// TODO: Add logback.
public class GameController implements UIManager {
    private static final Logger LOG = LoggerFactory.getLogger(GameController.class);
    private static final Joiner JOINER = Joiner.on(", ").skipNulls();
    public static final Image NOT_AVAILABLE = new Image(GameController.class.getResourceAsStream("/com/github/ykrasik/indexter/games/ui/not_available.png"));

    @FXML private CheckMenuItem showLog;
    @FXML private CheckMenuItem showSideBar;

    @FXML private TextField searchBox;
    @FXML private ComboBox<String> gameSort;

    @FXML private HBox contentScreen;
    @FXML private GridView<Game> gameWall;

    @FXML private TableView<Game> gamesTable;
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
    @FXML private Button statusBarStopButton;
    @FXML private TextArea logTextArea;

    @FXML private TableView<Library> libraries;
    @FXML private TableColumn<Library, String> libraryName;
    @FXML private TableColumn<Library, String> libraryPlatform;
    @FXML private TableColumn<Library, String> libraryPath;

    private Stage stage;
    private GameCollectionConfig config;
    private FlowManager flowManager;
    private GameManager gameManager;
    private LibraryManager libraryManager;

    private File prevDirectory;

    public void setDependencies(@NonNull Stage stage,
                                @NonNull GameCollectionConfig config,
                                @NonNull FlowManager flowManager,
                                @NonNull GameManager gameManager,
                                @NonNull LibraryManager libraryManager) {
        this.stage = stage;
        this.config = config;
        this.flowManager = flowManager;
        this.gameManager = gameManager;
        this.libraryManager = libraryManager;

        prevDirectory = config.getPrevDirectory().orElse(null);

        gameWall.itemsProperty().bind(gameManager.gamesProperty());
        gamesTable.itemsProperty().bind(gameManager.gamesProperty());
        libraries.itemsProperty().bind(libraryManager.librariesProperty());

        gameSort.setValue(GameSort.NAME.getKey());
    }

    // Called by JavaFx
    public void initialize() {
        initGameWall();
        initGameSearchBox();
        initGameSortBox();

        gameNameColumn.setCellValueFactory(e -> new SimpleStringProperty(e.getValue().getName()));
        gamePlatformColumn.setCellValueFactory(e -> new SimpleStringProperty(e.getValue().getPlatform().toString()));
        gameReleaseDateColumn.setCellValueFactory(e -> new SimpleStringProperty(toStringOrUnavailable(e.getValue().getReleaseDate())));
        gameCriticScoreColumn.setCellValueFactory(e -> new SimpleDoubleProperty(e.getValue().getCriticScore().orElse(0.0)));
        gameUserScoreColumn.setCellValueFactory(e -> new SimpleDoubleProperty(e.getValue().getUserScore().orElse(0.0)));
        gamePathColumn.setCellValueFactory(e -> new SimpleStringProperty(e.getValue().getPath().toString()));
        gameDateAddedColumn.setCellValueFactory(e -> new SimpleStringProperty(e.getValue().getLastModified().toLocalDate().toString()));

        gamesTable.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Game>() {
            @Override
            public void changed(ObservableValue<? extends Game> observable, Game oldValue, Game newValue) {
                if (newValue != null) {
                    displayGameOnSidePanel(newValue);
                }
            }
        });

        libraryName.setCellValueFactory(e -> new SimpleStringProperty(e.getValue().getName()));
        libraryPlatform.setCellValueFactory(e -> new SimpleStringProperty(e.getValue().getPlatform().toString()));
        libraryPath.setCellValueFactory(e -> new SimpleStringProperty(e.getValue().getPath().toString()));

//        libraries.setContextMenu(createLibraryContextMenu());

        showLog.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                bottomContainer.getChildren().add(0, logTextArea);
            } else {
                bottomContainer.getChildren().remove(logTextArea);
            }
        });

        showSideBar.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                contentScreen.getChildren().add(sideBar);
            } else {
                contentScreen.getChildren().remove(sideBar);
            }
        });
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
    }

    private void initGameSearchBox() {
        searchBox.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                if (newValue.isEmpty()) {
                    gameManager.noNameFilter();
                } else {
                    gameManager.nameFilter(newValue);
                }
            }
        });
    }

    private void initGameSortBox() {
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
    }

    private void displayGameOnSidePanel(Game game) {
        gamePath.setText(game.getPath().toString());

        poster.setImage(Optionals.or(game.getPoster(), game.getThumbnail()).map(ImageData::getImage).orElse(NOT_AVAILABLE));
        name.setText(game.getName());
        description.setText(toStringOrUnavailable(game.getDescription()));
        platform.setText(game.getPlatform().name());
        releaseDate.setText(toStringOrUnavailable(game.getReleaseDate()));
        criticScore.setText(toStringOrUnavailable(game.getCriticScore()));
        userScore.setText(toStringOrUnavailable(game.getUserScore()));
        genres.setText(JOINER.join(ListUtils.map(game.getGenres(), Genre::getName)));
        url.setText(game.getUrl());
    }

    private void addGameInfoCellContextMenu(GameInfoCell cell) {
        final ContextMenu contextMenu = new ContextMenu();

        final MenuItem deleteItem = new MenuItem("Delete");
        deleteItem.setOnAction(e -> {
            final Game game = cell.getItem();
            gameManager.deleteGame(game);
        });

        contextMenu.getItems().addAll(deleteItem);
        cell.setContextMenu(contextMenu);
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

    @Override
    public void updateProgress(int current, int total) {
        PlatformUtils.runLaterIfNecessary(() -> statusBar.setProgress((double) current / total));
    }

    @Override
    public void printMessage(String message) {
        PlatformUtils.runLaterIfNecessary(() -> {
            statusBar.setText(message);
            logTextArea.appendText(message);
            logTextArea.appendText("\n");
        });
        LOG.info(message);
    }

    @Override
    public void printMessage(String format, Object... args) {
        printMessage(String.format(format, args));
    }

    @Override
    public void configureStatusBarStopButton(Consumer<Button> statusBarStopButtonConfigurator) {
        statusBarStopButtonConfigurator.accept(statusBarStopButton);
    }

    @FXML
    public void addLibrary() {
        final DirectoryChooser directoryChooser = createDirectoryChooser("Add Library");
        final File selectedDirectory = directoryChooser.showDialog(stage);
        if (selectedDirectory != null) {
            prevDirectory = selectedDirectory;
            config.setPrevDirectory(prevDirectory);
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
                LOG.warn("Error adding library: " + path, e);
                createDialog().showException(e);
            }
        }
    }

    @FXML
    public void refreshLibraries() {
        flowManager.refreshLibraries(t -> {
            LOG.warn("Error refreshing libraries:", t);
            createDialog().title("Error refreshing libraries!").showException(t) ;
        });
    }

    @FXML
    public void cleanupGames() {
        flowManager.cleanupGames(t -> {
            LOG.warn("Error cleaning up games:", t);
            createDialog().title("Error cleaning up games!").showException(t) ;
        });
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

    private DirectoryChooser createDirectoryChooser(String title) {
        final DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle(title);
        directoryChooser.setInitialDirectory(prevDirectory);
        return directoryChooser;
    }

    private Dialogs createDialog() {
        return Dialogs.create().owner(stage);
    }
}
