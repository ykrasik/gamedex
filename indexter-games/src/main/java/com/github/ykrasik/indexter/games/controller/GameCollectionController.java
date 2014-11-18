package com.github.ykrasik.indexter.games.controller;

import com.github.ykrasik.indexter.exception.IndexterException;
import com.github.ykrasik.indexter.games.config.GameCollectionConfig;
import com.github.ykrasik.indexter.games.data.GameDataListener;
import com.github.ykrasik.indexter.games.data.GameDataService;
import com.github.ykrasik.indexter.games.datamodel.GameInfo;
import com.github.ykrasik.indexter.games.datamodel.GamePlatform;
import com.github.ykrasik.indexter.games.datamodel.Library;
import com.github.ykrasik.indexter.games.datamodel.LocalGameInfo;
import com.github.ykrasik.indexter.games.info.GameInfoService;
import com.github.ykrasik.indexter.games.library.LibraryManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.controlsfx.dialog.Dialogs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;

/**
 * @author Yevgeny Krasik
 */
public class GameCollectionController implements GameDataListener {
    private static final Logger LOG = LoggerFactory.getLogger(GameCollectionController.class);
    private static final Image NOT_AVAILABLE = new Image(GameCollectionController.class.getResourceAsStream("/com/github/ykrasik/indexter/games/ui/not_available.png"));

    @FXML private FlowPane gameWall;
    @FXML private ListView<String> gamesList;
    @FXML private ImageView thumbnail;
    @FXML private TextField path;
    @FXML private TextField name;
    @FXML private TextArea description;
    @FXML private TextField platform;
    @FXML private TextField releaseDate;
    @FXML private TextField criticScore;
    @FXML private TextField userScore;
    @FXML private TextField url;

    @FXML private TableView libraries;
    @FXML private TableColumn libraryName;
    @FXML private TableColumn libraryPlatform;
    @FXML private TableColumn libraryPath;

    private final Stage stage;
    private final GameCollectionConfig config;
    private final LibraryManager libraryManager;
    private final GameInfoService infoService;
    private final GameDataService dataService;
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
        this.searchController = new GameSearchController(stage, libraryManager, infoService, dataService);

        prevDirectory = config.getPrevDirectory().orElse(null);
    }

    // Called by JavaFx
    public void initialize() {
        LOG.debug("Populating initial data...");
        onUpdate(dataService.getAll());

        libraryName.setCellValueFactory(new PropertyValueFactory<Library, String>("name"));
        libraryPlatform.setCellValueFactory(new PropertyValueFactory<Library, String>("platform"));
        libraryPath.setCellValueFactory(new PropertyValueFactory<Library, String>("path"));
        libraries.setItems(FXCollections.observableArrayList(config.getLibraries().values()));
    }

    @Override
    public void onUpdate(Collection<LocalGameInfo> newOrUpdatedInfos) {

        final ObservableList<Node> children = gameWall.getChildren();
        for (LocalGameInfo info : newOrUpdatedInfos) {
            final ImageView imageView = createImageView(info);
//            final Image image = info.getGameInfo().getThumbnail().orElse(NOT_AVAILABLE);
//            gameWall.setCellFactory(gridView -> {
//                final ImageGridCell gridCell = new ImageGridCell();
//                gridCell.getStyleClass().add("gameTile");
//                gridCell.setOnMouseClicked(event -> {
//                    displayGameInfo(info);
////                    event.consume();
//                });
//                return gridCell;
//            });
            children.add(imageView);
        }

//        final List<Image> newChildren = ListUtils.map(newOrUpdatedInfos, this::createImage);


//        children.addAll(newChildren);

        for (LocalGameInfo info : newOrUpdatedInfos) {
            gamesList.getItems().add(info.getGameInfo().getName());
        }
        Collections.sort(gamesList.getItems());
    }

    private ImageView createImageView(LocalGameInfo info) {
        final ImageView imageView = new ImageView(info.getGameInfo().getThumbnail().orElse(NOT_AVAILABLE));
        imageView.setOnMouseClicked(event -> {
            displayGameInfo(info);
            event.consume();
        });
        imageView.getStyleClass().add("gameTile");
        return imageView;
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
                final Optional<Library> subLibrary = searchController.addGame(path, GamePlatform.PC);
                if (subLibrary.isPresent()) {
                    libraryManager.addSubLibraries(Collections.singletonList(subLibrary.get()));
                }
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

                // TODO: A bug in controlsFx returns null if the default value is selected.
                // TODO: When fixed, change the default value to PC.
                // FIXME: Show a select name dialog too.
                final GamePlatform platform = Dialogs.create()
                    .owner(stage)
                    .title("Choose library platform")
                    .masthead(path.toString())
                    .message("Choose library platform:")
                    .showChoices(GamePlatform.values());
                if (platform != null) {
                    libraryManager.addLibrary(new Library(path.getFileName().toString(), path, platform));
                }
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
