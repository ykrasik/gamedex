package com.github.ykrasik.indexter.games.controller;

import com.github.ykrasik.indexter.games.datamodel.GameBriefInfo;
import com.github.ykrasik.indexter.games.datamodel.GameDetailedInfo;
import com.github.ykrasik.indexter.games.datamodel.GamePlatform;
import com.github.ykrasik.indexter.games.info.GameInfoService;
import com.github.ykrasik.indexter.games.persistence.GameDataListener;
import com.github.ykrasik.indexter.games.persistence.GameDataService;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.TilePane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Yevgeny Krasik
 */
public class GameCollectionController implements GameDataListener {
    private final Stage stage;
    private final GameInfoService infoService;
    private final GameDataService dataService;

    @FXML
    private TilePane gameWall;

    @FXML
    private Button addGameButton;

    public GameCollectionController(Stage stage, GameInfoService infoService, GameDataService dataService) {
        this.stage = Objects.requireNonNull(stage);
        this.infoService = Objects.requireNonNull(infoService);
        this.dataService = Objects.requireNonNull(dataService);
    }

    @Override
    public void onUpdate(GameDataService dataService) {
        final Collection<GameDetailedInfo> infos = dataService.getAll();

        // FIXME: thumbnail should be cached.
        final List<ImageView> newChildren = infos.stream()
            .map(info -> new ImageView(new Image(info.getThumbnailUrl())))
            .collect(Collectors.toList());

        final ObservableList<Node> children = gameWall.getChildren();
        children.clear();
        children.addAll(newChildren);
    }

    // TODO: Encapsulate this.
    private File prevDirectory;
    @FXML
    public void addGame() {
        final DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Add game");
        directoryChooser.setInitialDirectory(prevDirectory);
        prevDirectory = directoryChooser.showDialog(stage);
        if (prevDirectory != null) {
            final String name = prevDirectory.getName();

            // FIXME: Do this in background thread.
            // FIXME: Platform should be a param
             addGame(name, GamePlatform.PC);
        }
    }

    private void addGame(String name, GamePlatform gamePlatform) {
        // FIXME: Handle exceptions
        try {
            final List<GameBriefInfo> briefInfos = infoService.searchGames(name, gamePlatform);
            if (briefInfos.isEmpty()) {
                throw new RuntimeException("Not found: " + name);
            }
            if (briefInfos.size() == 1) {
                final Optional<GameDetailedInfo> details = infoService.getDetails(briefInfos.get(0).getMoreDetailsId(), gamePlatform);
                if (!details.isPresent()) {
                    throw new RuntimeException("Specific search found nothing!!!");
                }

                dataService.add(details.get());
            } else {
                // TODO: Display a "choose specific"
                throw new RuntimeException("More then 1 option available: " + briefInfos);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
