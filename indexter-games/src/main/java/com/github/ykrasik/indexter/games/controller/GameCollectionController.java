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
import com.github.ykrasik.indexter.util.ListUtils;
import com.github.ykrasik.indexter.util.Optionals;
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

import static com.github.ykrasik.indexter.util.Optionals.toStringOrUnavailable;

/**
 * @author Yevgeny Krasik
 */
public class GameCollectionController {
    private static final Logger LOG = LoggerFactory.getLogger(GameCollectionController.class);
    private static final Joiner JOINER = Joiner.on(", ").skipNulls();
    public static final Image NOT_AVAILABLE = new Image(GameCollectionController.class.getResourceAsStream("/com/github/ykrasik/indexter/games/ui/not_available.png"));

    @FXML private BorderPane mainBorderPane;

    @FXML private GridView<Game> gameWall;

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
    @FXML private TextField genres;
    @FXML private Hyperlink url;

    @FXML private TableView<Game> gamesTable;
    @FXML private TableColumn<Game, String> gameNameColumn;
    @FXML private TableColumn<Game, String> gamePlatformColumn;
    @FXML private TableColumn<Game, String> gameReleaseDateColumn;
    @FXML private TableColumn<Game, Number> gameCriticScoreColumn;
    @FXML private TableColumn<Game, Number> gameUserScoreColumn;
    @FXML private TableColumn<Game, String> gamePathColumn;
    @FXML private TableColumn<Game, String> gameDateAddedColumn;

    @FXML private TableView<Library> libraries;
    @FXML private TableColumn<Library, String> libraryName;
    @FXML private TableColumn<Library, String> libraryPlatform;
    @FXML private TableColumn<Library, String> libraryPath;

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
        libraries.itemsProperty().bind(libraryManager.itemsProperty());

//        libraries.setContextMenu(createLibraryContextMenu());
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
        gameWall.itemsProperty().bind(gameManager.itemsProperty());
    }

    private void initGameSearchBox() {
        searchBox.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                if (newValue.isEmpty()) {
                    gameManager.unFilter();
                } else {
                    gameManager.filter(localGame -> StringUtils.containsIgnoreCase(localGame.getName(), newValue));
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
