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
import org.controlsfx.dialog.Dialog.Actions;
import org.controlsfx.dialog.Dialogs;
import org.controlsfx.dialog.Dialogs.CommandLink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
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
    // FIXME: Get a list of libraries.
    // FIXME: Instead of scanning children, add the directory as a sub-library.
    // Returns the new subLibraries.
    public void refreshLibraries() throws Exception {
        final List<Library> libraries = libraryManager.getLibraries();
        doRefreshLibraries(libraries);
    }

    private void doRefreshLibraries(List<Library> libraries) throws Exception {
        LOG.debug("Refreshing libraries: {}", libraries);
        final List<Library> newSubLibraries = new ArrayList<>();
        for (Library library : libraries) {
            final List<Library> subLibraries = refreshLibrary(library);
            newSubLibraries.addAll(subLibraries);
        }
        LOG.debug("Finished refreshing libraries.");

        if (!newSubLibraries.isEmpty()) {
            LOG.debug("New subLibraries detected: {}", newSubLibraries);
            libraryManager.addSubLibraries(newSubLibraries);
            doRefreshLibraries(newSubLibraries);
        }
    }

    private List<Library> refreshLibrary(Library library) throws Exception {
        LOG.debug("Refreshing library: {}", library);
        final List<Library> newSubLibraries = new ArrayList<>();
        final List<Path> directories = FileUtils.listChildDirectories(library.getPath());
        for (Path path : directories) {
            final Optional<Library> newSubLibrary = refreshPath(path, library.getPlatform());
            newSubLibrary.ifPresent(newSubLibraries::add);
        }
        LOG.debug("Finished refreshing library: {}.", library);
        return newSubLibraries;
    }

    private Optional<Library> refreshPath(Path path, GamePlatform platform) throws Exception {
        LOG.debug("Refreshing path: {}", path);

        if (libraryManager.isLibrary(path)) {
            LOG.debug("{} is a library, skipping...", path);
            return Optional.empty();
        }

        if (libraryManager.isExcluded(path)) {
            LOG.debug("{} is excluded, skipping...", path);
            return Optional.empty();
        }

        final Optional<LocalGameInfo> existingValue = dataService.get(path);
        if (existingValue.isPresent()) {
            LOG.debug("{} is already mapped, skipping...", path);
            return Optional.empty();
        }

        final Optional<Library> newSubLibrary = addGame(path, platform);
        LOG.debug("Finished refreshing {}.", path);
        return newSubLibrary;
    }

    public void scanDirectory(Path root, GamePlatform platform) throws Exception {
        LOG.debug("scanDirectory: root={}, platform={}", root, platform);
        final List<Path> directories = FileUtils.listChildDirectories(root);
        for (Path directory : directories) {
            addGame(directory, platform);
        }
    }

    public Optional<Library> addGame(Path path, GamePlatform platform) throws Exception {
        final String name = path.getFileName().toString();
        return addGame(path, name, platform);
    }

    private Optional<Library> addGame(Path path, String name, GamePlatform platform) throws Exception {
        LOG.debug("addGame: path={}, name={}, platform={}", path, name, platform);
        final List<GameRawBriefInfo> briefInfos = infoService.searchGames(name, platform);
        LOG.debug("Search found {} results.", briefInfos.size());

        if (briefInfos.isEmpty()) {
            return selectNewName(path, name, platform);
        }

        if (briefInfos.size() == 1) {
            doAddGame(path, briefInfos.get(0), platform);
            return Optional.empty();
        }

        final MultipleSearchResultChoice choice = showMultipleSearchResultDialog(path, name, briefInfos);
        switch (choice) {
            case SHOW:
                selectSearchResult(path, name, platform, briefInfos);
                break;

            case DIFFERENT_NAME:
                return selectNewName(path, name, platform);

            case DESIGNATE_SUB_LIBRARY:
                return designateSubLibrary(path, name, platform);

            case CANCEL:
                break;
                // Skip.
        }
        return Optional.empty();
    }

    private MultipleSearchResultChoice showMultipleSearchResultDialog(Path path, String name, List<GameRawBriefInfo> briefInfos) throws IOException {
        final CommandLink show = new CommandLink("Show", "Show all search results.");
        final CommandLink differentName = new CommandLink("Different name", "Retry with a different name.");
        final CommandLink scanChildren = new CommandLink("Designate as sub-library", "Designate this directory as a mini-library containing other entries.");

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
            multipleSearchResultChoice = MultipleSearchResultChoice.DESIGNATE_SUB_LIBRARY;
        } else {
            throw new IndexterException("Invalid choice: " + choice);
        }
        return multipleSearchResultChoice;
    }

    private Optional<Library> designateSubLibrary(Path path, String name, GamePlatform platform) {
        LOG.debug("Showing designate sub library dialog...");
        final Optional<Library> subLibrary = showDesignateSubLibraryDialog(path, name, platform);
        if (subLibrary.isPresent()) {
            LOG.debug("User created a new subLibrary: {}", subLibrary);
        } else {
            LOG.debug("User cancelled.");
        }
        return subLibrary;
    }

    private Optional<Library> showDesignateSubLibraryDialog(Path path, String name, GamePlatform platform) {
        final Optional<String> libraryName = Optional.ofNullable(Dialogs.create()
            .owner(stage)
            .title("Enter library name")
            .masthead(String.format("%s\nPlatform: %s\n", path.toString(), platform))
            .showTextInput(name)
        );
        return libraryName.map(libName -> new Library(libName, path, platform));
    }

    private Optional<Library> selectNewName(Path path, String name, GamePlatform gamePlatform) throws Exception {
        LOG.debug("Showing different name dialog...");
        final Optional<String> newName = showSelectNewNameDialog(path, name);
        if (newName.isPresent()) {
            LOG.debug("User chose a new name: '{}'", newName.get());
            return addGame(path, newName.get(), gamePlatform);
        } else {
            LOG.debug("User cancelled.");
            return Optional.empty();
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
        DESIGNATE_SUB_LIBRARY
    }
}
