package com.github.ykrasik.indexter.games.logic;

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
import javafx.application.Platform;
import javafx.beans.property.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * @author Yevgeny Krasik
 */
public class LogicManager {
    // FIXME: Log everything to a textual logger.
    private static final Logger LOG = LoggerFactory.getLogger(LogicManager.class);

    private final LibraryManager libraryManager;
    private final GameInfoService infoService;
    private final GameDataService dataService;

    private final StringProperty currentLibrary;
    private final StringProperty currentPath;
    private final StringProperty message;
    private final DoubleProperty refreshLibrariesProgress;
    private final DoubleProperty refreshLibraryProgress;

    public LogicManager(LibraryManager libraryManager,
                        GameInfoService infoService,
                        GameDataService dataService) {
        this.libraryManager = Objects.requireNonNull(libraryManager);
        this.infoService = Objects.requireNonNull(infoService);
        this.dataService = Objects.requireNonNull(dataService);

        this.currentLibrary = new SimpleStringProperty();
        this.currentPath = new SimpleStringProperty();
        this.message = new SimpleStringProperty();
        this.refreshLibrariesProgress = new SimpleDoubleProperty();
        this.refreshLibraryProgress = new SimpleDoubleProperty();
    }

    public StringProperty currentLibraryProperty() {
        return currentLibrary;
    }

    public StringProperty currentPathProperty() {
        return currentPath;
    }

    public StringProperty messageProperty() {
        return message;
    }

    public DoubleProperty refreshLibrariesProgressProperty() {
        return refreshLibrariesProgress;
    }

    public DoubleProperty refreshLibraryProgressProperty() {
        return refreshLibraryProgress;
    }

    public void refreshLibraries(ChoiceProvider choiceProvider) throws Exception {
        info("Refreshing libraries...");

        refreshLibrariesProgress.setValue(0);
        final List<Library> libraries = libraryManager.getLibraries();
        final int total = libraries.size();
        for (int i = 0; i < total; i++) {
            final Library library = libraries.get(i);
            refreshLibrary(library, choiceProvider);
            refreshLibrariesProgress.setValue((double)i / total);
        }

        info("Finished refreshing libraries.");
    }

    public void refreshLibrary(Library library, ChoiceProvider choiceProvider) throws Exception {
        info("Refreshing library: '%s'", library);
        currentLibrary.setValue(library.getName());

        refreshLibraryProgress.setValue(0);
        final List<Path> directories = FileUtils.listChildDirectories(library.getPath());
        final int total = directories.size();
        for (int i = 0; i < total; i++) {
            final Path path = directories.get(i);
            processPath(path, library.getPlatform(), choiceProvider);
            refreshLibraryProgress.setValue((double)i / total);
        }

        info("Finished refreshing library: '%s'", library);
    }

    public void processPath(Path path, GamePlatform platform, ChoiceProvider choiceProvider) throws Exception {
        LOG.debug("Processing path: {}...", path);
        currentPath.setValue(path.toString());

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
        addPath(path, name, platform, choiceProvider);
        LOG.debug("Finished Processing {}.", path);
    }

    private void addPath(Path path, String name, GamePlatform platform, ChoiceProvider choiceProvider) throws Exception {
        LOG.debug("addPath: path={}, name='{}', platform={}", path, name, platform);

        debug("Searching for '%s' with platform=%s...", name, platform);
        final List<GameRawBriefInfo> briefInfos = infoService.searchGames(name, platform);
        debug("Search: name='%s', platform=%s found %d results.", name, platform, briefInfos.size());

        if (briefInfos.isEmpty()) {
            handleNoSearchResults(path, name, platform, choiceProvider);
            return;
        }

        if (briefInfos.size() == 1) {
            doAddGame(path, briefInfos.get(0), platform);
            return;
        }

        handleMultipleSearchResults(path, name, platform, briefInfos, choiceProvider);
    }

    private void handleNoSearchResults(Path path, String name, GamePlatform platform, ChoiceProvider choiceProvider) throws Exception {
        final NoSearchResultsChoice choice = choiceProvider.getNoSearchResultsChoice(path, name, platform);
        switch (choice) {
            case NEW_NAME:
                selectNewName(path, name, platform, choiceProvider);
                break;

            case EXCLUDE:
                excludePath(path);
                break;

            case SKIP:
                break;
        }
    }

    private void handleMultipleSearchResults(Path path,
                                             String name,
                                             GamePlatform platform,
                                             List<GameRawBriefInfo> briefInfos,
                                             ChoiceProvider choiceProvider) throws Exception {
        final MultipleSearchResultsChoice choice = choiceProvider.getMultipleSearchResultsChoice(path, name, platform, briefInfos);
        switch (choice) {
            case CHOOSE:
                chooseFromMultipleSearchResults(path, name, platform, briefInfos, choiceProvider);
                break;

            case NEW_NAME:
                selectNewName(path, name, platform, choiceProvider);
                break;

            case EXCLUDE:
                excludePath(path);
                break;

            case SUB_LIBRARY:
                designateSubLibrary(path, name, platform, choiceProvider);
                break;

            case SKIP:
                break;
        }
    }

    private void chooseFromMultipleSearchResults(Path path,
                                                 String name,
                                                 GamePlatform platform,
                                                 List<GameRawBriefInfo> briefInfos,
                                                 ChoiceProvider choiceProvider) throws Exception {
        final Optional<GameRawBriefInfo> choice = choiceProvider.chooseFromMultipleResults(path, name, platform, briefInfos);
        if (choice.isPresent()) {
            doAddGame(path, choice.get(), platform);
        }
    }

    private void selectNewName(Path path, String name, GamePlatform platform, ChoiceProvider choiceProvider) throws Exception {
        final Optional<String> newName = choiceProvider.selectNewName(path, name, platform);
        if (newName.isPresent()) {
            addPath(path, newName.get(), platform, choiceProvider);
        }
    }

    private void designateSubLibrary(Path path, String name, GamePlatform platform, ChoiceProvider choiceProvider) throws Exception {
        final Optional<String> libraryName = choiceProvider.getSubLibraryName(path, name, platform);
        if (libraryName.isPresent()) {
            final Library subLibrary = new Library(libraryName.get(), path, platform);
            info("New sub-library created: %s", subLibrary);
            libraryManager.addSubLibrary(subLibrary);
            refreshLibrary(subLibrary, choiceProvider);
        }
    }

    private void doAddGame(Path path, GameRawBriefInfo briefInfo, GamePlatform gamePlatform) throws Exception {
        LOG.debug("Getting gameInfo from brief: {}", briefInfo);
        message.setValue(String.format("Fetching game info: '%s'...", briefInfo.getName()));

        final GameInfo gameInfo = infoService.getGameInfo(briefInfo.getMoreDetailsId(), gamePlatform).orElseThrow(
            () -> new IndexterException("Specific search found nothing: %s", briefInfo)
        );
        final LocalGameInfo info = new LocalGameInfo(path, gameInfo);
        dataService.add(info);
    }

    private void excludePath(Path path) {
        info("Excluding: %s", path);
        libraryManager.setExcluded(path);
    }

    private void info(String format, Object... args) {
        final String message = String.format(format, args);
        LOG.info(message);
        Platform.runLater(() -> this.message.setValue(message));
    }

    private void debug(String format, Object... args) {
        final String message = String.format(format, args);
        LOG.debug(message);
        Platform.runLater(() -> this.message.setValue(message));
    }
}
