package com.github.ykrasik.indexter.games.controller;

import com.github.ykrasik.indexter.exception.IndexterException;
import com.github.ykrasik.indexter.games.data.GameDataService;
import com.github.ykrasik.indexter.games.datamodel.GameInfo;
import com.github.ykrasik.indexter.games.datamodel.GamePlatform;
import com.github.ykrasik.indexter.games.info.GameInfoService;
import com.github.ykrasik.indexter.games.info.GameRawBriefInfo;
import com.github.ykrasik.indexter.util.FileUtils;
import javafx.stage.Stage;
import org.controlsfx.control.action.Action;
import org.controlsfx.dialog.Dialog.Actions;
import org.controlsfx.dialog.Dialogs;
import org.controlsfx.dialog.Dialogs.CommandLink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

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

        final CommandLink differentName = new CommandLink("Different name", "Retry with a different name.");
        final CommandLink showAll = new CommandLink("Show all", "Show all possibilities.");
        final CommandLink scanChildren = new CommandLink("Scan Children", "Treat this directory as a root directory containing more children.");

        final Action choice = Dialogs.create()
            .owner(stage)
            .title("Too many possibilities!")
            .masthead(String.format("%s\n\nToo many possibilities, choose an action:", directory))
            .message(String.format("Too many possibilities (%d) for '%s', choose an action:", briefInfos.size(), name))
            .showCommandLinks(differentName, differentName, showAll, scanChildren);

        if (choice != Actions.CANCEL) {
            if (choice == differentName) {
                showDifferentNameDialog(directory, name, platform);
            } else if (choice == showAll) {
                showAllPossibilities(directory, platform, briefInfos);
            } else if (choice == scanChildren) {
                scanDirectory(directory, platform);
            } else {
                throw new IndexterException("Invalid choice: " + choice);
            }
        }
    }

    private void showDifferentNameDialog(Path directory, String name, GamePlatform gamePlatform) throws Exception {
        final String newName = Dialogs.create()
            .owner(stage)
            .title("Couldn't find game!")
            .masthead(String.format("%s\n\nCouldn't find game: '%s'\nEnter new name or cancel to skip.", directory, name))
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

    private void doAddGame(GameRawBriefInfo briefInfo, GamePlatform gamePlatform) throws Exception {
        final GameInfo info = infoService.getGameInfo(briefInfo.getMoreDetailsId(), gamePlatform).orElseThrow(
            () -> new IndexterException("Specific search found nothing: %s", briefInfo)
        );
        dataService.add(info);
    }
}
