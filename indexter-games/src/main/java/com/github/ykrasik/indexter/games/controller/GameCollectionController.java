package com.github.ykrasik.indexter.games.controller;

import com.github.ykrasik.indexter.exception.IndexterException;
import com.github.ykrasik.indexter.games.config.GameCollectionPreferences;
import com.github.ykrasik.indexter.games.data.GameDataListener;
import com.github.ykrasik.indexter.games.data.GameDataService;
import com.github.ykrasik.indexter.games.datamodel.GameInfo;
import com.github.ykrasik.indexter.games.datamodel.GamePlatform;
import com.github.ykrasik.indexter.games.datamodel.LocalGameInfo;
import com.github.ykrasik.indexter.games.info.GameInfoService;
import com.github.ykrasik.indexter.util.ListUtils;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.TilePane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.controlsfx.dialog.Dialogs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * @author Yevgeny Krasik
 */
public class GameCollectionController implements GameDataListener {
    private static final Logger LOG = LoggerFactory.getLogger(GameCollectionController.class);
    private static final Image NOT_AVAILABLE = new Image(GameCollectionController.class.getResourceAsStream("/com/github/ykrasik/indexter/games/ui/not_available.png"));

    @FXML private TilePane gameWall;
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

    private final Stage stage;
    private final GameCollectionPreferences preferences;
    private final GameInfoService infoService;
    private final GameDataService dataService;
    private final GameSearchController searchController;

    private File prevDirectory;

    public GameCollectionController(Stage stage,
                                    GameCollectionPreferences preferences,
                                    GameInfoService infoService,
                                    GameDataService dataService) {
        this.stage = Objects.requireNonNull(stage);
        this.preferences = Objects.requireNonNull(preferences);
        this.infoService = Objects.requireNonNull(infoService);
        this.dataService = Objects.requireNonNull(dataService);
        this.searchController = new GameSearchController(stage, infoService, dataService);

        prevDirectory = preferences.getPrevDirectory().orElse(null);
    }

    // Called by JavaFx
    public void initialize() {
        LOG.debug("Populating initial data...");
        onUpdate(dataService.getAll());
    }

    @Override
    public void onUpdate(Collection<LocalGameInfo> newOrUpdatedInfos) {
        final List<ImageView> newChildren = ListUtils.map(newOrUpdatedInfos, this::createImageView);

        final ObservableList<Node> children = gameWall.getChildren();
        children.addAll(newChildren);

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
            preferences.setPrevDirectory(prevDirectory);
            final Path path = Paths.get(selectedDirectory.toURI());
            try {
                // FIXME: Do this in background thread.
                // FIXME: Platform should be a param
                searchController.addGame(path, GamePlatform.PC);
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
            preferences.setPrevDirectory(prevDirectory);
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
            preferences.setPrevDirectory(prevDirectory);
            final Path path = Paths.get(selectedDirectory.toURI());
            try {
                final Map<Path, GamePlatform> libraries = preferences.getLibraries();
                if (libraries.containsKey(path)) {
                    throw new IndexterException("Already have a library defined for '%s'", path);
                }

                // TODO: A bug in controlsFx returns null if the default value is selected.
                // TODO: When fixed, change the default value to PC.
                final GamePlatform platform = Dialogs.create()
                    .owner(stage)
                    .title("Choose library platform")
                    .masthead(path.toString())
                    .message("Choose library platform:")
                    .showChoices(GamePlatform.values());
                if (platform != null) {
                    libraries.put(path, platform);
                    preferences.setLibraries(libraries);
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
            final Map<Path, GamePlatform> libraries = preferences.getLibraries();
            searchController.refreshLibraries(libraries);
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
