package com.github.ykrasik.indexter.games.controller;

import com.github.ykrasik.indexter.exception.IndexterException;
import com.github.ykrasik.indexter.games.data.GameDataService;
import com.github.ykrasik.indexter.games.datamodel.GameInfo;
import com.github.ykrasik.indexter.games.datamodel.GamePlatform;
import com.github.ykrasik.indexter.games.datamodel.Library;
import com.github.ykrasik.indexter.games.datamodel.LocalGameInfo;
import com.github.ykrasik.indexter.games.info.GameInfoService;
import com.github.ykrasik.indexter.games.info.GameRawBriefInfo;
import com.github.ykrasik.indexter.games.library.LibraryManager;
import com.github.ykrasik.indexter.util.FileUtils;
import javafx.stage.Stage;
import org.controlsfx.control.action.Action;
import org.controlsfx.dialog.Dialog;
import org.controlsfx.dialog.DialogAction;
import org.controlsfx.dialog.Dialogs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.*;

/**
 * @author Yevgeny Krasik
 */
public class GameSearchController {
    private static final Logger LOG = LoggerFactory.getLogger(GameSearchController.class);

    private final Stage stage;
    private final LibraryManager libraryManager;
    private final GameInfoService infoService;
    private final GameDataService dataService;

    public GameSearchController(Stage stage,
                                LibraryManager libraryManager,
                                GameInfoService infoService,
                                GameDataService dataService) {
        this.stage = Objects.requireNonNull(stage);
        this.libraryManager = Objects.requireNonNull(libraryManager);
        this.infoService = Objects.requireNonNull(infoService);
        this.dataService = Objects.requireNonNull(dataService);
    }

    // FIXME: Add ability to exclude directories.
    public void refreshLibraries() throws Exception {
        LOG.info("Refreshing libraries...");
        final List<Library> libraries = libraryManager.getLibraries();
        for (Library library : libraries) {
            refreshLibrary(library);
        }
        LOG.debug("Finished refreshing libraries.");
    }

    private void refreshLibrary(Library library) throws Exception {
        LOG.debug("Refreshing library: {}...", library);
        final List<Path> directories = FileUtils.listChildDirectories(library.getPath());
        for (Path path : directories) {
            processPath(path, library.getPlatform());
        }
        LOG.debug("Finished refreshing library: {}.", library);
    }

    public void processPath(Path path, GamePlatform platform) throws Exception {
        LOG.debug("Processing path: {}...", path);

        if (libraryManager.isLibrary(path)) {
            LOG.debug("{} is a library, skipping...", path);
            return;
        }

        // TODO: Excludes should belong to the containig library, not the libraryManager.
        if (libraryManager.isExcluded(path)) {
            LOG.debug("{} is excluded, skipping...", path);
            return;
        }

        final Optional<LocalGameInfo> existingValue = dataService.get(path);
        if (existingValue.isPresent()) {
            LOG.debug("{} is already mapped, skipping...", path);
            return;
        }

        final String name = path.getFileName().toString();
        addPath(path, name, platform);
        LOG.debug("Finished Processing {}.", path);
    }

    private void addPath(Path path, String name, GamePlatform platform) throws Exception {
        LOG.debug("addPath: path={}, name={}, platform={}", path, name, platform);
        final List<GameRawBriefInfo> briefInfos = infoService.searchGames(name, platform);
        LOG.debug("Search found {} results.", briefInfos.size());

        if (briefInfos.isEmpty()) {
            handleNoSearchResults(path, name, platform);
            return;
        }

        if (briefInfos.size() == 1) {
            doAddGame(path, briefInfos.get(0), platform);
            return;
        }

        handleMultipleSearchResults(path, name, platform, briefInfos);
    }

    private void handleNoSearchResults(Path path, String name, GamePlatform platform) throws Exception {
        final DialogAction newName = new DialogAction("New name");
        newName.setLongText("Retry with a new name");

        final DialogAction exclude = new DialogAction("Exclude");
        exclude.setLongText("Exclude directory from further processing");

        LOG.debug("Showing no search results dialog...");
        final Action choice = Dialogs.create()
            .owner(stage)
            .title("No search results found!")
            .masthead(path.toString())
            .message("No search results found!")
            .showCommandLinks(newName, exclude);

        if (choice == Dialog.ACTION_CANCEL) {
            LOG.debug("User canceled.");
        } else if (choice == newName) {
            selectNewName(path, name, platform);
        } else if (choice == exclude) {
            LOG.info("Excluding: '{}'", path);
            libraryManager.setExcluded(path);
        } else {
            throw new IndexterException("Invalid choice: %s", choice);
        }
    }

    private void handleMultipleSearchResults(Path path,
                                             String name,
                                             GamePlatform platform,
                                             List<GameRawBriefInfo> briefInfos) throws Exception {
        final DialogAction chooseOne = new DialogAction("Choose");
        chooseOne.setLongText("Choose from the search results");

        final DialogAction exclude = new DialogAction("Exclude");
        exclude.setLongText("Exclude directory from further processing");

        final DialogAction newName = new DialogAction("New name");
        newName.setLongText("Retry with a new name");

        final DialogAction designateSubLibrary = new DialogAction("Designate as sub-library");
        designateSubLibrary.setLongText("Scan this directory's children");

        final List<DialogAction> choices = new ArrayList<>();
        choices.add(chooseOne);
        choices.add(exclude);
        choices.add(newName);
        if (FileUtils.hasChildDirectories(path)) {
            choices.add(designateSubLibrary);
        }

        LOG.debug("Showing multiple search result dialog...");
        final Action choice = Dialogs.create()
            .owner(stage)
            .title("Too many search results!")
            .masthead(path.toString())
            .message(String.format("Found %d search results for '%s':", briefInfos.size(), name))
            .showCommandLinks(choices);

        if (choice == Dialog.ACTION_CANCEL) {
            LOG.debug("User canceled.");
        } else if (choice == chooseOne) {
            chooseFromMultipleSearchResults(path, name, platform, briefInfos);
        } else if (choice == exclude) {
            LOG.info("Excluding: '{}'", path);
            libraryManager.setExcluded(path);
        } else if (choice == newName) {
            selectNewName(path, name, platform);
        } else if (choice == designateSubLibrary) {
            designateSubLibrary(path, name, platform);
        } else {
            throw new IndexterException("Invalid choice: %s", choice);
        }
    }

    private void chooseFromMultipleSearchResults(Path path,
                                                 String name,
                                                 GamePlatform platform,
                                                 List<GameRawBriefInfo> briefInfos) throws Exception {
        LOG.debug("Showing all search results...");

        final Optional<GameRawBriefInfo> choice = Dialogs.create()
            .owner(stage)
            .title(String.format("Search results for: '%s'", name))
            .masthead(path.toString())
            .showChoices(briefInfos);

        if (!choice.isPresent()) {
            LOG.debug("Show all search results dialog cancelled.");
            return;
        }

        LOG.info("Choice from multiple results: '{}'", choice.get());
        doAddGame(path, choice.get(), platform);
    }

    private void selectNewName(Path path, String name, GamePlatform gamePlatform) throws Exception {
        LOG.debug("Showing new name dialog...");

        final Optional<String> newName = Dialogs.create()
            .owner(stage)
            .title(String.format("Couldn't find game: '%s'", name))
            .masthead(path.toString())
            .showTextInput(name);

        if (!newName.isPresent()) {
            LOG.debug("Select new name dialog cancelled.");
            return;
        }

        LOG.info("New name chosen: '{}'", newName.get());
        addPath(path, newName.get(), gamePlatform);
    }

    private void designateSubLibrary(Path path, String name, GamePlatform platform) throws Exception {
        LOG.debug("Showing designate sub-library dialog...");

        final Optional<String> libraryName = Dialogs.create()
            .owner(stage)
            .title("Enter library name")
            .masthead(String.format("%s\nPlatform: %s\n", path.toString(), platform))
            .showTextInput(name);

        if (!libraryName.isPresent()) {
            LOG.debug("Create sub-library dialog cancelled.");
            return;
        }

        final Library subLibrary = new Library(libraryName.get(), path, platform);
        LOG.info("New sub-library created: {}", subLibrary);
        libraryManager.addSubLibrary(subLibrary);
        refreshLibrary(subLibrary);
    }

    private void doAddGame(Path path, GameRawBriefInfo briefInfo, GamePlatform gamePlatform) throws Exception {
        LOG.debug("Getting gameInfo from brief: {}", briefInfo);
        final GameInfo gameInfo = infoService.getGameInfo(briefInfo.getMoreDetailsId(), gamePlatform).orElseThrow(
            () -> new IndexterException("Specific search found nothing: %s", briefInfo)
        );
        final LocalGameInfo info = new LocalGameInfo(path, gameInfo);
        dataService.add(info);
    }

    public void scanDirectory(Path root, GamePlatform platform) throws Exception {
        LOG.debug("scanDirectory: root={}, platform={}", root, platform);
        final Library tempLibrary = new Library("tempScan", root, platform);
        refreshLibrary(tempLibrary);
    }
}
