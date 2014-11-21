package com.github.ykrasik.indexter.games.controller;

import com.github.ykrasik.indexter.AbstractService;
import com.github.ykrasik.indexter.exception.IndexterException;
import com.github.ykrasik.indexter.games.config.GameCollectionConfig;
import com.github.ykrasik.indexter.games.data.GameDataService;
import com.github.ykrasik.indexter.games.datamodel.*;
import com.github.ykrasik.indexter.games.info.GameInfoService;
import com.github.ykrasik.indexter.games.library.LibraryManager;
import com.github.ykrasik.indexter.games.logic.LogicManager;
import com.github.ykrasik.indexter.games.ui.GameInfoCell;
import com.github.ykrasik.indexter.ui.FixedRating;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.controlsfx.control.GridView;
import org.controlsfx.control.StatusBar;
import org.controlsfx.dialog.Dialogs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;

/**
 * @author Yevgeny Krasik
 */
public class GameCollectionController extends AbstractService {
    private static final Logger LOG = LoggerFactory.getLogger(GameCollectionController.class);
    public static final Image NOT_AVAILABLE = new Image(GameCollectionController.class.getResourceAsStream("/com/github/ykrasik/indexter/games/ui/not_available.png"));

    @FXML private BorderPane mainBorderPane;

    @FXML private GridView<LocalGameInfo> gameWall;

    @FXML private ComboBox<GameSort> gameWallSort;

    @FXML private ImageView thumbnail;
    @FXML private TextField path;
    @FXML private TextField name;
    @FXML private TextArea description;
    @FXML private TextField platform;
    @FXML private TextField releaseDate;
    @FXML private TextField criticScore;
    @FXML private TextField userScore;
    @FXML private TextField url;

    @FXML private TableView<LocalGameInfo> gamesTable;
    @FXML private TableColumn<LocalGameInfo, String> gameNameColumn;
    @FXML private TableColumn<LocalGameInfo, String> gamePlatformColumn;
    @FXML private TableColumn<LocalGameInfo, String> gameReleaseDateColumn;
    @FXML private TableColumn<LocalGameInfo, String> gameCriticScoreColumn;
    @FXML private TableColumn<LocalGameInfo, FixedRating> gameCriticScoreVisualColumn;
    @FXML private TableColumn<LocalGameInfo, String> gameUserScoreColumn;
    @FXML private TableColumn<LocalGameInfo, FixedRating> gameUserScoreVisualColumn;
    @FXML private TableColumn<LocalGameInfo, String> gamePathColumn;

    @FXML private TableView libraries;
    @FXML private TableColumn libraryName;
    @FXML private TableColumn libraryPlatform;
    @FXML private TableColumn libraryPath;

    private final Stage stage;
    private final GameCollectionConfig config;
    private final LibraryManager libraryManager;
    private final GameInfoService infoService;
    private final GameDataService dataService;

    private final StatusBar statusBar;
    private final GameSearchController searchController;

    private File prevDirectory;

    public GameCollectionController(Stage stage,
                                    GameCollectionConfig config,
                                    LibraryManager libraryManager,
                                    GameInfoService infoService,
                                    GameDataService dataService) {
        this.stage = Objects.requireNonNull(stage);
        this.config = Objects.requireNonNull(config);
        this.libraryManager = Objects.requireNonNull(libraryManager);
        this.infoService = Objects.requireNonNull(infoService);
        this.dataService = Objects.requireNonNull(dataService);

        this.statusBar = new StatusBar();
        statusBar.setText("Welcome to inDexter!");

        // FIXME: Create with Spring.
        final LogicManager logicManager = new LogicManager(libraryManager, infoService, dataService);
        this.searchController = new GameSearchController(stage, statusBar, logicManager);

        prevDirectory = config.getPrevDirectory().orElse(null);
    }

    @Override
    protected void doStart() throws Exception {
        searchController.start();
    }

    @Override
    protected void doStop() throws Exception {
        searchController.stop();
    }

    // Called by JavaFx
    public void initialize() {
        mainBorderPane.setBottom(statusBar);

        gameWall.itemsProperty().bind(dataService.itemsProperty());
        gameWall.setCellFactory(param -> {
            final GameInfoCell cell = new GameInfoCell();
            cell.getStyleClass().add("gameTile");
            cell.setOnMouseClicked(event -> {
                final LocalGameInfo info = cell.getItem();
                displayGameInfo(info);
                event.consume();
            });
            return cell;
        });

        gameWallSort.setItems(FXCollections.observableArrayList(GameSort.values()));
        gameWallSort.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<GameSort>() {
            @Override
            public void changed(ObservableValue<? extends GameSort> observable, GameSort oldValue, GameSort newValue) {
                final Comparator<LocalGameInfo> comparator;
                switch (newValue) {
                    case DATE_ADDED: comparator = GameComparators.dateAddedComparator(); break;
                    case NAME: comparator = GameComparators.nameComparator(); break;
                    case CRITIC_SCORE: comparator = GameComparators.criticScoreComparator(); break;
                    case USER_SCORE: comparator = GameComparators.userScoreComparator(); break;
                    case RELEASE_DATE: comparator = GameComparators.releaseDateComparator(); break;
                    default: throw new IndexterException("Invalid sort value: %s", newValue);
                }

                // FIXME: This sucks a lot.
                FXCollections.sort(dataService.itemsProperty().get(), comparator);
                gameWall.itemsProperty().unbind();
                gameWall.setItems(FXCollections.emptyObservableList());
                gameWall.itemsProperty().bind(dataService.itemsProperty());
            }
        });

        gamesTable.itemsProperty().bind(dataService.itemsProperty());
        gameNameColumn.setCellValueFactory(e -> new SimpleStringProperty(e.getValue().getGameInfo().getName()));
        gamePlatformColumn.setCellValueFactory(e -> new SimpleStringProperty(e.getValue().getGameInfo().getGamePlatform().toString()));
        gameReleaseDateColumn.setCellValueFactory(e -> new SimpleStringProperty(e.getValue().getGameInfo().getReleaseDate().map(LocalDate::toString).orElse("Unavailable")));
        gameCriticScoreColumn.setCellValueFactory(e -> new SimpleStringProperty(String.valueOf(e.getValue().getGameInfo().getCriticScore())));
        gameCriticScoreVisualColumn.setCellValueFactory(e -> {
            final FixedRating rating = new FixedRating(5);
            rating.setPartialRating(true);
            rating.setRating(e.getValue().getGameInfo().getCriticScore() / 100 * 5);
            return new SimpleObjectProperty<>(rating);
        });
        gameUserScoreColumn.setCellValueFactory(e -> new SimpleStringProperty(String.valueOf(e.getValue().getGameInfo().getUserScore())));
        gamePathColumn.setCellValueFactory(e -> new SimpleStringProperty(e.getValue().getPath().toString()));

        libraryName.setCellValueFactory(new PropertyValueFactory<Library, String>("name"));
        libraryPlatform.setCellValueFactory(new PropertyValueFactory<Library, String>("platform"));
        libraryPath.setCellValueFactory(new PropertyValueFactory<Library, String>("path"));
        libraries.setItems(FXCollections.observableArrayList(config.getLibraries().values()));

    }

    private void displayGameInfo(LocalGameInfo localInfo) {
        final Path path = localInfo.getPath();
        this.path.setText(path.toString());

        final GameInfo gameInfo = localInfo.getGameInfo();
        thumbnail.setImage(gameInfo.getThumbnail().orElse(NOT_AVAILABLE));
        name.setText(gameInfo.getName());
        description.setText(gameInfo.getDescription().orElse("No description available."));
        platform.setText(gameInfo.getGamePlatform().name());
        releaseDate.setText(gameInfo.getReleaseDate().map(Object::toString).orElse("Unavailable."));
        criticScore.setText(String.valueOf(gameInfo.getCriticScore()));
        userScore.setText(String.valueOf(gameInfo.getUserScore()));
        url.setText(gameInfo.getUrl().orElse("Unavailable."));
    }

    @FXML
    public void showAddGameDialog() {
        final DirectoryChooser directoryChooser = createDirectoryChooser("Add Game");
        final File selectedDirectory = directoryChooser.showDialog(stage);
        if (selectedDirectory != null) {
            prevDirectory = selectedDirectory;
            config.setPrevDirectory(prevDirectory);
            final Path path = Paths.get(selectedDirectory.toURI());
            try {
                // FIXME: Do this in background thread.
                // FIXME: Platform should be a param
                searchController.processPath(path, GamePlatform.PC);
            } catch (Exception e) {
                LOG.warn("Error adding game: " + path, e);
                Dialogs.create().owner(stage).showException(e);
            }
        }
    }

    @FXML
    public void showScanDirectoryDialog() {
        final DirectoryChooser directoryChooser = createDirectoryChooser("Scan Directory");
        final File selectedDirectory = directoryChooser.showDialog(stage);
        if (selectedDirectory != null) {
            prevDirectory = selectedDirectory;
            config.setPrevDirectory(prevDirectory);
            final Path root = Paths.get(selectedDirectory.toURI());
            try {
                // FIXME: Do this in background thread.
                // FIXME: Platform should be a param
                searchController.scanDirectory(root, GamePlatform.PC);
            } catch (Exception e) {
                LOG.warn("Error scanning directory: " + root, e);
                Dialogs.create().owner(stage).showException(e);
            }
        }
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
                final Optional<GamePlatform> platform = Dialogs.create()
                    .owner(stage)
                    .title("Choose library platform")
                    .masthead(path.toString())
                    .message("Choose library platform:")
                    .showChoices(GamePlatform.values());
                platform.ifPresent(p -> libraryManager.addLibrary(new Library(path.getFileName().toString(), path, p)));
            } catch (Exception e) {
                LOG.warn("Error adding library: " + path, e);
                Dialogs.create().owner(stage).showException(e);
            }
        }
    }

    @FXML
    public void refreshLibraries() {
        try {
            searchController.refreshLibraries();
        } catch (Exception e) {
            LOG.warn("Error refreshing libraries:", e);
            Dialogs.create().owner(stage).showException(e);
        }
    }

    private DirectoryChooser createDirectoryChooser(String title) {
        final DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle(title);
        directoryChooser.setInitialDirectory(prevDirectory);
        return directoryChooser;
    }
}
