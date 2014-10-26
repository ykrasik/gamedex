package com.github.ykrasik.indexter.games.controller;

import com.github.ykrasik.indexter.games.config.GameCollectionPreferences;
import com.github.ykrasik.indexter.games.data.GameDataListener;
import com.github.ykrasik.indexter.games.data.GameDataService;
import com.github.ykrasik.indexter.games.datamodel.GameInfo;
import com.github.ykrasik.indexter.games.datamodel.GamePlatform;
import com.github.ykrasik.indexter.games.info.GameInfoService;
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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author Yevgeny Krasik
 */
public class GameCollectionController implements GameDataListener {
    private static final Logger LOG = LoggerFactory.getLogger(GameCollectionController.class);
    private static final Image NOT_AVAILABLE = new Image(GameCollectionController.class.getResourceAsStream("/com/github/ykrasik/indexter/games/ui/not_available.png"));

    @FXML private TilePane gameWall;
    @FXML private ListView<String> gamesList;
    @FXML private ImageView thumbnail;
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
    public void onUpdate(Collection<GameInfo> newOrUpdatedInfos) {
        final List<ImageView> newChildren = newOrUpdatedInfos.stream().map(this::createImageView).collect(Collectors.toList());

        final ObservableList<Node> children = gameWall.getChildren();
        children.addAll(newChildren);

        for (GameInfo info : newOrUpdatedInfos) {
            gamesList.getItems().add(info.getName());
        }
        Collections.sort(gamesList.getItems());
    }

    private ImageView createImageView(GameInfo info) {
        final ImageView imageView = new ImageView(info.getThumbnail().orElse(NOT_AVAILABLE));
        imageView.setOnMouseClicked(event -> {
            displayGameInfo(info);
            event.consume();
        });
        imageView.getStyleClass().add("gameTile");
        return imageView;
    }

    private void displayGameInfo(GameInfo info) {
        thumbnail.setImage(info.getThumbnail().orElse(NOT_AVAILABLE));
        name.setText(info.getName());
        description.setText(info.getDescription().orElse("No description available."));
        platform.setText(info.getGamePlatform().name());
        releaseDate.setText(info.getReleaseDate().map(Object::toString).orElse("Unavailable."));
        criticScore.setText(String.valueOf(info.getCriticScore()));
        userScore.setText(String.valueOf(info.getUserScore()));
        url.setText(info.getUrl().orElse("Unavailable."));
    }

    @FXML
    public void showAddGameDialog() {
        final DirectoryChooser directoryChooser = createDirectoryChooser("Add Game");
        final File selectedDirectory = directoryChooser.showDialog(stage);
        if (selectedDirectory != null) {
            prevDirectory = selectedDirectory;
            preferences.setPrevDirectory(prevDirectory);
            final Path directory = Paths.get(selectedDirectory.toURI());
            try {
                // FIXME: Do this in background thread.
                // FIXME: Platform should be a param
                searchController.addGame(directory, GamePlatform.PC);
            } catch (Exception e) {
                LOG.warn("Error adding game: " + directory, e);
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
    private DirectoryChooser createDirectoryChooser(String title) {
        final DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle(title);
        directoryChooser.setInitialDirectory(prevDirectory);
        return directoryChooser;
    }

}
