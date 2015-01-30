package com.github.ykrasik.indexter.games.controller;

import com.github.ykrasik.indexter.exception.IndexterException;
import com.github.ykrasik.indexter.games.config.GameCollectionConfig;
import com.github.ykrasik.indexter.games.datamodel.*;
import com.github.ykrasik.indexter.games.manager.flow.FlowManager;
import com.github.ykrasik.indexter.games.manager.game.GameManager;
import com.github.ykrasik.indexter.games.manager.library.LibraryManager;
import com.github.ykrasik.indexter.games.ui.GameInfoCell;
import com.github.ykrasik.indexter.util.Optionals;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.apache.commons.lang3.StringUtils;
import org.controlsfx.control.GridView;
import org.controlsfx.dialog.Dialogs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Optional;

/**
 * @author Yevgeny Krasik
 */
public class GameCollectionController {
    private static final Logger LOG = LoggerFactory.getLogger(GameCollectionController.class);
    public static final Image NOT_AVAILABLE = new Image(GameCollectionController.class.getResourceAsStream("/com/github/ykrasik/indexter/games/ui/not_available.png"));

    @FXML private BorderPane mainBorderPane;

    @FXML private GridView<LocalGame> gameWall;

    @FXML private ComboBox<String> gameSort;

    @FXML private TextField searchBox;

    // TODO: Display total number of games somewhere.
    // TODO: Allow changing thumbnail & poster via right-click.
    // TODO: Disable sort on list view, and set default sort to name based.
    // TODO: Add detail view on double click
    // TODO: Side bar should be hideable
    // TODO: Add hideable bottom text logger
    // TODO: Add metacritic Url
    // TODO: Save Genres properly.
    // TODO: Allow filtering by genre type.
    // TODO: Add right-click menus to library list.
    // TODO: Add logback.

    // FIXME: Keep each tab in it's own FXML.
    @FXML private ImageView poster;
    @FXML private TextField gamePath;
    @FXML private TextField name;
    @FXML private TextArea description;
    @FXML private TextField platform;
    @FXML private TextField releaseDate;
    @FXML private TextField criticScore;
    @FXML private TextField userScore;

    @FXML private TableView<LocalGame> gamesTable;
    @FXML private TableColumn<LocalGame, String> gameNameColumn;
    @FXML private TableColumn<LocalGame, String> gamePlatformColumn;
    @FXML private TableColumn<LocalGame, String> gameReleaseDateColumn;
    @FXML private TableColumn<LocalGame, Number> gameCriticScoreColumn;
    @FXML private TableColumn<LocalGame, Number> gameUserScoreColumn;
    @FXML private TableColumn<LocalGame, String> gamePathColumn;
    @FXML private TableColumn<LocalGame, String> gameDateAddedColumn;

    @FXML private TableView<LocalLibrary> libraries;
    @FXML private TableColumn<LocalLibrary, String> libraryName;
    @FXML private TableColumn<LocalLibrary, String> libraryPlatform;
    @FXML private TableColumn<LocalLibrary, String> libraryPath;

    private final Stage stage;
    private final GameCollectionConfig config;
    private final FlowManager flowManager;
    private final GameManager gameManager;
    private final LibraryManager libraryManager;

    private File prevDirectory;

    public GameCollectionController(Stage stage,
                                    GameCollectionConfig config,
                                    FlowManager flowManager,
                                    GameManager gameManager,
                                    LibraryManager libraryManager) {
        this.stage = Objects.requireNonNull(stage);
        this.config = Objects.requireNonNull(config);
        this.flowManager = Objects.requireNonNull(flowManager);
        this.gameManager = Objects.requireNonNull(gameManager);
        this.libraryManager = Objects.requireNonNull(libraryManager);

        prevDirectory = config.getPrevDirectory().orElse(null);
    }

    // Called by JavaFx
    public void initialize() {
        mainBorderPane.setBottom(flowManager.getStatusBar());

        initGameWall();
        initGameSearchBox();
        initGameSortBox();

        gamesTable.itemsProperty().bind(gameManager.itemsProperty());
        gameNameColumn.setCellValueFactory(e -> new SimpleStringProperty(e.getValue().getGame().getName()));
        gamePlatformColumn.setCellValueFactory(e -> new SimpleStringProperty(e.getValue().getGame().getPlatform().toString()));
        gameReleaseDateColumn.setCellValueFactory(e -> new SimpleStringProperty(toString(e.getValue().getGame().getReleaseDate())));
        gameCriticScoreColumn.setCellValueFactory(e -> new SimpleDoubleProperty(e.getValue().getGame().getCriticScore().orElse(0.0)));
        gameUserScoreColumn.setCellValueFactory(e -> new SimpleDoubleProperty(e.getValue().getGame().getUserScore().orElse(0.0)));
        gamePathColumn.setCellValueFactory(e -> new SimpleStringProperty(e.getValue().getPath().toString()));
        gameDateAddedColumn.setCellValueFactory(e -> new SimpleStringProperty(e.getValue().getLastModified().toLocalDate().toString()));

        gamesTable.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<LocalGame>() {
            @Override
            public void changed(ObservableValue<? extends LocalGame> observable, LocalGame oldValue, LocalGame newValue) {
                if (newValue != null) {
                    displayGameOnSidePanel(newValue);
                }
            }
        });

        libraryName.setCellValueFactory(e -> new SimpleStringProperty(e.getValue().getLibrary().getName()));
        libraryPlatform.setCellValueFactory(e -> new SimpleStringProperty(e.getValue().getLibrary().getPlatform().toString()));
        libraryPath.setCellValueFactory(e -> new SimpleStringProperty(e.getValue().getLibrary().getPath().toString()));
        libraries.itemsProperty().bind(libraryManager.itemsProperty());

//        libraries.setContextMenu(createLibraryContextMenu());
    }

    private void initGameWall() {
        gameWall.setCellFactory(param -> {
            final GameInfoCell cell = new GameInfoCell();
            cell.getStyleClass().add("gameTile");
            cell.setOnMouseClicked(event -> {
                final LocalGame game = cell.getItem();
                displayGameOnSidePanel(game);
                event.consume();
            });

            addGameInfoCellContextMenu(cell);
            return cell;
        });
        gameWall.itemsProperty().bind(gameManager.itemsProperty());
    }

    private void initGameSearchBox() {
        searchBox.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                if (newValue.isEmpty()) {
                    gameManager.unFilter();
                } else {
                    gameManager.filter(localGame -> StringUtils.containsIgnoreCase(localGame.getGame().getName(), newValue));
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
        gameSort.setValue(GameSort.DATE_ADDED.getKey());
    }

    private void displayGameOnSidePanel(LocalGame localGame) {
        final Path path = localGame.getPath();
        gamePath.setText(path.toString());

        final Game game = localGame.getGame();
        poster.setImage(Optionals.or(game.getPoster(), game.getThumbnail()).orElse(NOT_AVAILABLE));
        name.setText(game.getName());
        description.setText(toString(game.getDescription()));
        platform.setText(game.getPlatform().name());
        releaseDate.setText(toString(game.getReleaseDate()));
        criticScore.setText(toString(game.getCriticScore()));
        userScore.setText(toString(game.getUserScore()));
    }

    private void addGameInfoCellContextMenu(GameInfoCell cell) {
        final ContextMenu contextMenu = new ContextMenu();

        final MenuItem deleteItem = new MenuItem("Delete");
        deleteItem.setOnAction(e -> {
            final LocalGame game = cell.getItem();
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

    private <T> String toString(Optional<T> optional) {
        return Optionals.toString(optional, "Unavailable");
    }

    @FXML
    public void showAddLibraryDialog() {
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
                platform.ifPresent(p -> libraryManager.addLibrary(new Library(path.getFileName().toString(), path, p)));
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
