package com.github.ykrasik.indexter.games.controller;

import com.github.ykrasik.indexter.exception.IndexterException;
import com.github.ykrasik.indexter.games.config.GameCollectionConfig;
import com.github.ykrasik.indexter.games.datamodel.*;
import com.github.ykrasik.indexter.games.manager.game.GameManager;
import com.github.ykrasik.indexter.games.manager.library.LibraryManager;
import com.github.ykrasik.indexter.games.manager.scan.ScanManager;
import com.github.ykrasik.indexter.games.ui.GameInfoCell;
import com.github.ykrasik.indexter.ui.FixedRating;
import com.github.ykrasik.indexter.util.Optionals;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
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

    @FXML private ComboBox<GameSort> gameWallSort;

    @FXML private ImageView thumbnail;
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
    @FXML private TableColumn<LocalGame, FixedRating> gameCriticScoreVisualColumn;
    @FXML private TableColumn<LocalGame, Number> gameUserScoreColumn;
    @FXML private TableColumn<LocalGame, FixedRating> gameUserScoreVisualColumn;
    @FXML private TableColumn<LocalGame, String> gamePathColumn;

    @FXML private TableView<LocalLibrary> libraries;
    @FXML private TableColumn<LocalLibrary, String> libraryName;
    @FXML private TableColumn<LocalLibrary, String> libraryPlatform;
    @FXML private TableColumn<LocalLibrary, String> libraryPath;

    private final Stage stage;
    private final GameCollectionConfig config;
    private final ScanManager scanManager;
    private final GameManager gameManager;
    private final LibraryManager libraryManager;

    private File prevDirectory;

    public GameCollectionController(Stage stage,
                                    GameCollectionConfig config,
                                    ScanManager scanManager,
                                    GameManager gameManager,
                                    LibraryManager libraryManager) {
        this.stage = Objects.requireNonNull(stage);
        this.config = Objects.requireNonNull(config);
        this.scanManager = Objects.requireNonNull(scanManager);
        this.gameManager = Objects.requireNonNull(gameManager);
        this.libraryManager = Objects.requireNonNull(libraryManager);

        prevDirectory = config.getPrevDirectory().orElse(null);
    }

    // Called by JavaFx
    public void initialize() {
        mainBorderPane.setBottom(scanManager.getStatusBar());

        gameWall.setCellFactory(param -> {
            final GameInfoCell cell = new GameInfoCell();
            cell.getStyleClass().add("gameTile");
            cell.setOnMouseClicked(event -> {
                final LocalGame info = cell.getItem();
                displayGameInfo(info);
                event.consume();
            });

            final ContextMenu contextMenu = new ContextMenu();
            final MenuItem deleteItem = new MenuItem("Delete");
            deleteItem.setOnAction(e -> {
                final LocalGame game = cell.getItem();
                gameManager.deleteGame(game);
            });
            contextMenu.getItems().addAll(deleteItem);
            cell.setContextMenu(contextMenu);
            return cell;
        });

//        gameWall.setItems(gameManager.getAllGames());
        gameWall.itemsProperty().bind(gameManager.itemsProperty());

        gameWallSort.setItems(FXCollections.observableArrayList(GameSort.values()));
        gameWallSort.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<GameSort>() {
            @Override
            public void changed(ObservableValue<? extends GameSort> observable, GameSort oldValue, GameSort newValue) {
                gameManager.sort(newValue);
//                gameWall.itemsProperty().unbind();
//                gameWall.setItems(FXCollections.emptyObservableList());
//                gameWall.itemsProperty().bind(dataService.itemsProperty());
            }
        });

        gamesTable.itemsProperty().bind(gameManager.itemsProperty());
        gameNameColumn.setCellValueFactory(e -> new SimpleStringProperty(e.getValue().getGame().getName()));
        gamePlatformColumn.setCellValueFactory(e -> new SimpleStringProperty(e.getValue().getGame().getPlatform().toString()));
        gameReleaseDateColumn.setCellValueFactory(e -> new SimpleStringProperty(toString(e.getValue().getGame().getReleaseDate())));
        gameCriticScoreColumn.setCellValueFactory(e -> new SimpleDoubleProperty(e.getValue().getGame().getCriticScore().orElse(0.0)));
        gameCriticScoreVisualColumn.setCellValueFactory(e -> {
            final FixedRating rating = new FixedRating(5);
            rating.setPartialRating(true);
            rating.setRating(e.getValue().getGame().getCriticScore().orElse(0.0) / 100 * 5);
            return new SimpleObjectProperty<>(rating);
        });
        gameUserScoreColumn.setCellValueFactory(e -> new SimpleDoubleProperty(e.getValue().getGame().getUserScore().orElse(0.0)));
        gamePathColumn.setCellValueFactory(e -> new SimpleStringProperty(e.getValue().getPath().toString()));

        libraryName.setCellValueFactory(e -> new SimpleStringProperty(e.getValue().getLibrary().getName()));
        libraryPlatform.setCellValueFactory(e -> new SimpleStringProperty(e.getValue().getLibrary().getPlatform().toString()));
        libraryPath.setCellValueFactory(e -> new SimpleStringProperty(e.getValue().getLibrary().getPath().toString()));
        libraries.itemsProperty().bind(libraryManager.itemsProperty());
    }

    private void displayGameInfo(LocalGame localGame) {
        final Path path = localGame.getPath();
        this.gamePath.setText(path.toString());

        final Game game = localGame.getGame();
        thumbnail.setImage(Optionals.or(game.getPoster(), game.getThumbnail()).orElse(NOT_AVAILABLE));
        name.setText(game.getName());
        description.setText(toString(game.getDescription()));
        platform.setText(game.getPlatform().name());
        releaseDate.setText(toString(game.getReleaseDate()));
        criticScore.setText(toString(game.getCriticScore()));
        userScore.setText(toString(game.getUserScore()));
    }

    private <T> String toString(Optional<T> optional) {
        return Optionals.toString(optional, "Unavailable");
    }

//    @FXML
//    public void showAddGameDialog() {
//        final DirectoryChooser directoryChooser = createDirectoryChooser("Add Game");
//        final File selectedDirectory = directoryChooser.showDialog(stage);
//        if (selectedDirectory != null) {
//            prevDirectory = selectedDirectory;
//            config.setPrevDirectory(prevDirectory);
//            final Path path = Paths.get(selectedDirectory.toURI());
//            try {
//                // FIXME: Do this in background thread.
//                // FIXME: Platform should be a param
//                scanManager.processPath(path, GamePlatform.PC);
//            } catch (Exception e) {
//                LOG.warn("Error adding game: " + path, e);
//                createDialog().showException(e);
//            }
//        }
//    }

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
        scanManager.refreshLibraries(t -> {
            LOG.warn("Error refreshing libraries:", t);
            createDialog().title("Error refreshing libraries!").showException(t) ;
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
