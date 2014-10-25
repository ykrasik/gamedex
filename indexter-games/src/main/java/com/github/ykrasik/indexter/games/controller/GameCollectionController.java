package com.github.ykrasik.indexter.games.controller;

import com.github.ykrasik.indexter.games.datamodel.GameInfo;
import com.github.ykrasik.indexter.games.info.GameRawBriefInfo;
import com.github.ykrasik.indexter.games.datamodel.GamePlatform;
import com.github.ykrasik.indexter.games.info.GameInfoService;
import com.github.ykrasik.indexter.games.data.GameDataListener;
import com.github.ykrasik.indexter.games.data.GameDataService;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ListView;
import javafx.scene.image.ImageView;
import javafx.scene.layout.TilePane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.controlsfx.dialog.Dialogs;
import org.controlsfx.dialog.Dialogs.CommandLink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Yevgeny Krasik
 */
public class GameCollectionController implements GameDataListener {
    private static final Logger LOG = LoggerFactory.getLogger(GameCollectionController.class);

    private final Stage stage;
    private final GameInfoService infoService;
    private final GameDataService dataService;

    @FXML
    private TilePane gameWall;

    @FXML
    private ListView<String> gamesList;

    public GameCollectionController(Stage stage, GameInfoService infoService, GameDataService dataService) {
        this.stage = Objects.requireNonNull(stage);
        this.infoService = Objects.requireNonNull(infoService);
        this.dataService = Objects.requireNonNull(dataService);
    }

    // Called by JavaFx
    public void initialize() {
        LOG.debug("Populating initial data...");
        onUpdate(dataService.getAll());
    }

    @Override
    public void onUpdate(Collection<GameInfo> newOrUpdatedInfos) {

        // FIXME: thumbnail should be cached.
        final List<ImageView> newChildren = newOrUpdatedInfos.stream()
            .map(info -> new ImageView(info.getThumbnail()))
            .collect(Collectors.toList());

        final ObservableList<Node> children = gameWall.getChildren();
        children.addAll(newChildren);

        for (GameInfo info : newOrUpdatedInfos) {
            gamesList.getItems().add(info.getName());
        }
        Collections.sort(gamesList.getItems());
    }

    @FXML
    public void scanDirectory() throws IOException {
        final DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Add game");
        directoryChooser.setInitialDirectory(prevDirectory);
        prevDirectory = directoryChooser.showDialog(stage);
        if (prevDirectory != null) {
            final String name = prevDirectory.getName();

            // FIXME: Do this in background thread.
            // FIXME: Platform should be a param
            final Path path = Paths.get(prevDirectory.toURI());
            try (Stream<Path> list = Files.list(path).filter(Files::isDirectory)) {
                list.forEach(dir -> addGame(dir.getFileName().toString(), GamePlatform.PC));
            }
            addGame(name, GamePlatform.PC);
        }
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
            final List<GameRawBriefInfo> briefInfos = infoService.searchGames(name, gamePlatform);
            if (briefInfos.isEmpty()) {
                throw new RuntimeException("Not found: " + name);
            }
            if (briefInfos.size() == 1) {
                doAddGame(briefInfos.get(0), gamePlatform);
            } else {
                final List<CommandLink> possibilities = briefInfos.stream()
                    .map(brief -> new CommandLinkWithItem<>(brief.getName(), serializeBriefInfo(brief), brief))
                    .collect(Collectors.toList());

                final CommandLinkWithItem<GameRawBriefInfo> choice = (CommandLinkWithItem<GameRawBriefInfo>) Dialogs.create()
                    .title("Please choose one:")
                    .masthead("Please choose one:")
                    .message("Please choose one:")
                    .showCommandLinks(possibilities.get(0), possibilities);

                doAddGame(choice.getItem(), gamePlatform);
            }
        } catch (Exception e) {
            e.printStackTrace();
            LOG.warn("Error: ", e);
        }
    }

    private void doAddGame(GameRawBriefInfo briefInfo, GamePlatform gamePlatform) throws Exception {
        final GameInfo info = infoService.getGameInfo(briefInfo.getMoreDetailsId(), gamePlatform).orElseThrow(
            () -> new RuntimeException("Specific search found nothing!!!")
        );

        dataService.add(info);
    }

    private String serializeBriefInfo(GameRawBriefInfo briefInfo) {
        return
            "Release date: " + briefInfo.getReleaseDate() + '\n' +
            "Score: " + briefInfo.getScore();
    }

    private static class CommandLinkWithItem<T> extends CommandLink {
        private final T item;

        public CommandLinkWithItem(Node graphic, String text, String longText, T item) {
            super(graphic, text, longText);
            this.item = item;
        }

        public CommandLinkWithItem(String message, String comment, T item) {
            super(message, comment);
            this.item = item;
        }

        public T getItem() {
            return item;
        }
    }
}
