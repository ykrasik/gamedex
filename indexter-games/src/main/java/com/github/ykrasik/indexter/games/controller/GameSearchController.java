package com.github.ykrasik.indexter.games.controller;

import com.github.ykrasik.indexter.exception.IndexterException;
import com.github.ykrasik.indexter.games.data.GameDataService;
import com.github.ykrasik.indexter.games.datamodel.GameInfo;
import com.github.ykrasik.indexter.games.datamodel.GamePlatform;
import com.github.ykrasik.indexter.games.datamodel.LocalGameInfo;
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

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.Map.Entry;

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

    public void refreshLibraries(Map<Path, GamePlatform> libraries) throws Exception {
        LOG.debug("Refreshing libraries: {}", libraries);
        for (Entry<Path, GamePlatform> entry : libraries.entrySet()) {
            final Path libraryRoot = entry.getKey();
            final GamePlatform platform = entry.getValue();

            LOG.debug("Refreshing library: {} -> {}", libraryRoot, platform);
            final List<Path> directories = FileUtils.listChildDirectories(libraryRoot);
            for (Path path : directories) {
                refreshPath(path, platform, libraries);
            }
            LOG.debug("Finished refreshing library: {} -> {}", libraryRoot, platform);
        }
    }

    private void refreshPath(Path path, GamePlatform platform, Map<Path, GamePlatform> libraries) throws Exception {
        LOG.debug("Refreshing {}...", path);
        if (libraries.containsKey(path)) {
            LOG.debug("{} is a library, skipping...", path);
            return;
        }

        final Optional<LocalGameInfo> existingValue = dataService.get(path);
        if (existingValue.isPresent()) {
            LOG.debug("{} already exists, skipping...", path);
            return;
        }

        addGame(path, platform);
        LOG.debug("Finished refreshing {}.", path);
    }

    public void scanDirectory(Path root, GamePlatform platform) throws Exception {
        LOG.debug("scanDirectory: root={}, platform={}", root, platform);
        final List<Path> directories = FileUtils.listChildDirectories(root);
        for (Path directory : directories) {
            addGame(directory, platform);
        }
    }

    public void addGame(Path path, GamePlatform platform) throws Exception {
        final String name = path.getFileName().toString();
        addGame(path, name, platform);
    }

    private void addGame(Path path, String name, GamePlatform platform) throws Exception {
        LOG.debug("addGame: path={}, name={}, platform={}", path, name, platform);
        final List<GameRawBriefInfo> briefInfos = infoService.searchGames(name, platform);
        LOG.debug("Search found {} results.", briefInfos.size());

        if (briefInfos.isEmpty()) {
            selectNewName(path, name, platform);
            return;
        }

        if (briefInfos.size() == 1) {
            doAddGame(path, briefInfos.get(0), platform);
            return;
        }

        final MultipleSearchResultChoice choice = showMultipleSearchResultDialog(path, name, briefInfos);
        switch (choice) {
            case SHOW:
                selectSearchResult(path, name, platform, briefInfos);
                break;

            case DIFFERENT_NAME:
                selectNewName(path, name, platform);
                break;

            case SCAN_CHILDREN:
                scanDirectory(path, platform);
                break;

            case CANCEL:
                // Skip.
        }
    }

    private MultipleSearchResultChoice showMultipleSearchResultDialog(Path path, String name, List<GameRawBriefInfo> briefInfos) throws IOException {
        final CommandLink show = new CommandLink("Show", "Show all search results.");
        final CommandLink differentName = new CommandLink("Different name", "Retry with a different name.");
        final CommandLink scanChildren = new CommandLink("Scan Children", "Treat this directory as a root that contains more children.");

        final List<CommandLink> choices = new ArrayList<>();
        choices.add(show);
        choices.add(differentName);
        if (FileUtils.hasChildDirectories(path)) {
            choices.add(scanChildren);
        }

        LOG.debug("Showing multiple search result dialog...");
        final Action choice = Dialogs.create()
            .owner(stage)
            .title("Too many search results!")
            .masthead(path.toString())
            .message(String.format("Found %d search results for '%s':", briefInfos.size(), name))
            .showCommandLinks(show, choices);

        final MultipleSearchResultChoice multipleSearchResultChoice;
        if (choice == Actions.CANCEL) {
            LOG.debug("User canceled.");
            multipleSearchResultChoice = MultipleSearchResultChoice.CANCEL;
        } else if (choice == show) {
            LOG.debug("User chose to show search results.");
            multipleSearchResultChoice = MultipleSearchResultChoice.SHOW;
        } else if (choice == differentName) {
            LOG.debug("User chose to select a different name.");
            multipleSearchResultChoice = MultipleSearchResultChoice.DIFFERENT_NAME;
        } else if (choice == scanChildren) {
            LOG.debug("User chose to scan children.");
            multipleSearchResultChoice = MultipleSearchResultChoice.SCAN_CHILDREN;
        } else {
            throw new IndexterException("Invalid choice: " + choice);
        }
        return multipleSearchResultChoice;
    }

    private void selectNewName(Path path, String name, GamePlatform gamePlatform) throws Exception {
        LOG.debug("Showing different name dialog...");
        final Optional<String> newName = showSelectNewNameDialog(path, name);
        if (newName.isPresent()) {
            LOG.debug("User chose a new name: '{}'", newName.get());
            addGame(path, newName.get(), gamePlatform);
        } else {
            LOG.debug("User cancelled.");
        }
    }

    private Optional<String> showSelectNewNameDialog(Path path, String name) {
        return Optional.ofNullable(Dialogs.create()
            .owner(stage)
            .title(String.format("Couldn't find game: '%s'", name))
            .masthead(path.toString())
            .showTextInput(name));
    }

    private void selectSearchResult(Path path, String name, GamePlatform platform, List<GameRawBriefInfo> briefInfos) throws Exception {
        LOG.debug("Showing all search results...");
        final Optional<GameRawBriefInfo> choice = showSelectResultDialog(path, name, briefInfos);
        if (choice.isPresent()) {
            LOG.debug("User chose: '{}'", choice.get());
            doAddGame(path, choice.get(), platform);
        } else {
            LOG.debug("User cancelled.");
        }
    }

    private Optional<GameRawBriefInfo> showSelectResultDialog(Path path, String name, List<GameRawBriefInfo> briefInfos) {
        return Optional.ofNullable(Dialogs.create()
            .owner(stage)
            .title(String.format("Search results for: '%s'", name))
            .masthead(path.toString())
            .showChoices(briefInfos));
    }

    private void doAddGame(Path path, GameRawBriefInfo briefInfo, GamePlatform gamePlatform) throws Exception {
        LOG.debug("Getting gameInfo from brief: {}", briefInfo);
        final GameInfo gameInfo = infoService.getGameInfo(briefInfo.getMoreDetailsId(), gamePlatform).orElseThrow(
            () -> new IndexterException("Specific search found nothing: %s", briefInfo)
        );
        final LocalGameInfo info = new LocalGameInfo(path, gameInfo);
        dataService.add(info);
    }

    private enum MultipleSearchResultChoice {
        CANCEL,
        SHOW,
        DIFFERENT_NAME,
        SCAN_CHILDREN
    }
}
