package com.github.ykrasik.indexter.games.controller;

import com.github.ykrasik.indexter.exception.IndexterException;
import com.github.ykrasik.indexter.games.data.GameDataService;
import com.github.ykrasik.indexter.games.datamodel.GameInfo;
import com.github.ykrasik.indexter.games.datamodel.GamePlatform;
import com.github.ykrasik.indexter.games.info.GameInfoService;
import com.github.ykrasik.indexter.games.info.GameRawBriefInfo;
import com.github.ykrasik.indexter.util.FileUtils;
import com.github.ykrasik.indexter.util.UrlUtils;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import org.controlsfx.control.action.Action;
import org.controlsfx.dialog.Dialog.Actions;
import org.controlsfx.dialog.Dialogs;
import org.controlsfx.dialog.Dialogs.CommandLink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Yevgeny Krasik
 */
public class GameSearchController {
    private static final Logger LOG = LoggerFactory.getLogger(GameSearchController.class);

    private final Stage stage;
    private final GameInfoService infoService;
    private final GameDataService dataService;

    public GameSearchController(Stage stage, GameInfoService infoService, GameDataService dataService) {
        this.stage = Objects.requireNonNull(stage);
        this.infoService = Objects.requireNonNull(infoService);
        this.dataService = Objects.requireNonNull(dataService);
    }

    public void scanDirectory(Path root, GamePlatform platform) throws Exception {
        final List<Path> directories = FileUtils.listChildDirectories(root);
        for (Path directory : directories) {
            addGame(directory, platform);
        }
    }

    public void addGame(Path directory, GamePlatform platform) throws Exception {
        final String name = directory.getFileName().toString();
        addGame(directory, name, platform);
    }

    private void addGame(Path directory, String name, GamePlatform platform) throws Exception {
        final List<GameRawBriefInfo> briefInfos = infoService.searchGames(name, platform);
        if (briefInfos.isEmpty()) {
            showDifferentNameDialog(directory, name, platform);
            return;
        }

        if (briefInfos.size() == 1) {
            doAddGame(briefInfos.get(0), platform);
            return;
        }

        final List<CommandLink> possibilities = briefInfos.stream()
            .limit(3).map(this::createChoiceCommandLink).collect(Collectors.toList());
        final CommandLink rename = new CommandLink("Rename", "Retry with a different name.");
        final CommandLink showAll = new CommandLink("Show all", "Show all possibilities.");
        final CommandLink scanChildren = new CommandLink("Scan Children", "Treat this directory as a root directory containing more children.");
        possibilities.add(0, rename);
        possibilities.add(1, showAll);
        possibilities.add(2, scanChildren);

        final String masthead = String.format("%s\n\nToo many possibilities, choose an action:", directory);
        final Action choice = Dialogs.create()
            .owner(stage)
            .title("Too many possibilities!")
            .masthead(masthead)
            .message(String.format("Too many possibilities(%d) for '%s', choose an action:", briefInfos.size(), name))
            .showCommandLinks(rename, possibilities);

        if (choice != Actions.CANCEL) {
            if (choice == rename) {
                showDifferentNameDialog(directory, name, platform);
            } else if (choice == showAll) {
                showAllPossibilities(directory, platform, briefInfos);
            } else if (choice == scanChildren) {
                scanDirectory(directory, platform);
            } else {
                final GameRawBriefInfo choiceItem = ((CommandLinkWithItem<GameRawBriefInfo>) choice).item;
                doAddGame(choiceItem, platform);
            }
        }
    }

    private void showDifferentNameDialog(Path directory, String name, GamePlatform gamePlatform) throws Exception {
        final String masthead = String.format("%s\n\nCouldn't find game: '%s'\nEnter new name or cancel to skip.", directory, name);
        final String newName = Dialogs.create()
            .owner(stage)
            .title("Couldn't find game!")
            .masthead(masthead)
            .showTextInput(name);

        if (newName != null) {
            addGame(directory, newName, gamePlatform);
        }
    }

    private void showAllPossibilities(Path directory, GamePlatform platform, List<GameRawBriefInfo> briefInfos) throws Exception {
        final GameRawBriefInfo choice = Dialogs.create()
            .owner(stage)
            .title("Please choose one:")
            .masthead(String.format("%s\n\nPlease choose one or cancel to skip.", directory))
            .showChoices(briefInfos);

        if (choice != null) {
            doAddGame(choice, platform);
        }
    }

    private CommandLinkWithItem<GameRawBriefInfo> createChoiceCommandLink(GameRawBriefInfo briefInfo) {
        try {
            final Optional<String> tinyImageUrl = briefInfo.getTinyImageUrl();
            final ImageView imageView;
            if (tinyImageUrl.isPresent()) {
                final byte[] bytes = UrlUtils.fetchData(tinyImageUrl.get());
                imageView = new ImageView(new Image(new ByteArrayInputStream(bytes)));
            } else {
                imageView = null;
            }

            final String name = briefInfo.getName();
            final String description = String.format("Release date: %s\nScore: %s",
                briefInfo.getReleaseDate().map(Object::toString).orElse("Not available"), briefInfo.getScore());
            if (imageView != null) {
                return new CommandLinkWithItem<>(imageView, name, description, briefInfo);
            } else {
                return new CommandLinkWithItem<>(name, description, briefInfo);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void doAddGame(GameRawBriefInfo briefInfo, GamePlatform gamePlatform) throws Exception {
        final GameInfo info = infoService.getGameInfo(briefInfo.getMoreDetailsId(), gamePlatform).orElseThrow(
            () -> new IndexterException("Specific search found nothing: %s", briefInfo)
        );
        dataService.add(info);
    }

    private static class CommandLinkWithItem<T> extends CommandLink {
        private final T item;

        private CommandLinkWithItem(Node graphic, String text, String longText, T item) {
            super(graphic, text, longText);
            this.item = item;
        }

        private CommandLinkWithItem(String message, String comment, T item) {
            super(message, comment);
            this.item = item;
        }
    }
}
