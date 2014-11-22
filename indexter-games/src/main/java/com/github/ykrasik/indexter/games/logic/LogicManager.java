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
import com.github.ykrasik.indexter.util.Optionals;
import javafx.application.Platform;
import javafx.beans.property.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
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
    private final GameInfoService metacriticInfoService;
    private final GameInfoService giantBombInfoService;
    private final GameDataService dataService;

    // FIXME: All these cannot be accessed from here, but through Platform.runLater.
    private final StringProperty currentLibrary;
    private final StringProperty currentPath;
    private final StringProperty message;
    private final DoubleProperty refreshLibrariesProgress;
    private final DoubleProperty refreshLibraryProgress;

    public LogicManager(LibraryManager libraryManager,
                        GameInfoService metacriticInfoService,
                        GameInfoService giantBombInfoService,
                        GameDataService dataService) {
        this.libraryManager = Objects.requireNonNull(libraryManager);
        this.metacriticInfoService = Objects.requireNonNull(metacriticInfoService);
        this.giantBombInfoService = Objects.requireNonNull(giantBombInfoService);
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

        Platform.runLater(() -> refreshLibrariesProgress.setValue(0));
        final List<Library> libraries = libraryManager.getLibraries();
        final int total = libraries.size();
        for (int i = 0; i < total; i++) {
            final Library library = libraries.get(i);
            refreshLibrary(library, choiceProvider);
            // FIXME: Why do I need to call platform.later?
            final double current = i;
            Platform.runLater(() -> refreshLibrariesProgress.setValue(current / total));
        }

        info("Finished refreshing libraries.");
    }

    public void refreshLibrary(Library library, ChoiceProvider choiceProvider) throws Exception {
        info("Refreshing library: '%s'", library);
        Platform.runLater(() -> currentLibrary.setValue(library.getName()));

        Platform.runLater(() -> refreshLibraryProgress.setValue(0));
        final List<Path> directories = FileUtils.listChildDirectories(library.getPath());
        final int total = directories.size();
        for (int i = 0; i < total; i++) {
            final Path path = directories.get(i);
            processPath(path, library.getPlatform(), choiceProvider);
            // FIXME: Why do I need to call platform.later?
            final double current = i;
            Platform.runLater(() -> refreshLibraryProgress.setValue(current / total));
        }

        info("Finished refreshing library: '%s'", library);
    }

    public void processPath(Path path, GamePlatform platform, ChoiceProvider choiceProvider) throws Exception {
        LOG.debug("Processing path: {}...", path);
        Platform.runLater(() -> currentPath.setValue(path.toString()));

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

        final Optional<GameInfo> metacriticGameInfo = getMetacriticGameInfo(path, name, platform, choiceProvider);
        if (metacriticGameInfo.isPresent()) {
            LOG.debug("Metacritic gameInfo: {}", metacriticGameInfo);
            final Optional<GameInfo> giantBombGameInfo = getGiantBombGameInfo(path, metacriticGameInfo.get().getName(), platform, choiceProvider);
            final GameInfo gameInfo;
            if (giantBombGameInfo.isPresent()) {
                LOG.debug("GiantBomb gameInfo: {}", giantBombGameInfo);
                gameInfo = mergeGameInfo(metacriticGameInfo.get(), giantBombGameInfo.get());
            } else {
                LOG.debug("GiantBomb gameInfo not found.");
                gameInfo = metacriticGameInfo.get();
            }

            // FIXME: Find a better solution other then assigning 0 to id.
            final LocalGameInfo info = new LocalGameInfo(0, path, gameInfo);
            dataService.add(info);
        }
    }

    private Optional<GameInfo> getMetacriticGameInfo(Path path, String name, GamePlatform platform, ChoiceProvider choiceProvider) throws Exception {
        debug("Searching Metacritic for '%s'[%s]...", name, platform);
        final List<GameRawBriefInfo> briefInfos = metacriticInfoService.searchGames(name, platform);
        debug("Metacritic Search: '%s'[%s] found %d results.", name, platform, briefInfos.size());

        if (briefInfos.isEmpty()) {
            return handleNoMetacriticSearchResults(path, name, platform, choiceProvider);
        }

        if (briefInfos.size() > 1) {
            return handleMultipleMetacriticSearchResults(path, name, platform, briefInfos, choiceProvider);
        }

        final GameRawBriefInfo briefInfo = briefInfos.get(0);
        return fetchMetacriticGameInfo(briefInfo, platform);
    }

    private Optional<GameInfo> getGiantBombGameInfo(Path path,
                                                    String name,
                                                    GamePlatform platform,
                                                    ChoiceProvider choiceProvider) throws Exception {
        debug("Searching GiantBomb for '%s'[%s]...", name, platform);
        final List<GameRawBriefInfo> briefInfos = giantBombInfoService.searchGames(name, platform);
        debug("GiantBomb Search: '%s'[%s] found %d results.", name, platform, briefInfos.size());

        if (briefInfos.isEmpty()) {
            return selectNewGiantBombName(path, name, platform, choiceProvider);
        }

        if (briefInfos.size() > 1) {
            return handleMultipleGiantBombSearchResults(path, name, platform, briefInfos, choiceProvider);
        }

        final GameRawBriefInfo briefInfo = briefInfos.get(0);
        return fetchGiantBombGameInfo(briefInfo, platform);
    }

    private Optional<GameInfo> handleNoMetacriticSearchResults(Path path,
                                                               String name,
                                                               GamePlatform platform,
                                                               ChoiceProvider choiceProvider) throws Exception {
        final NoSearchResultsChoice choice = choiceProvider.getNoMetacriticSearchResultsChoice(path, name, platform);
        switch (choice) {
            case NEW_NAME:
                return selectNewMetacriticName(path, name, platform, choiceProvider);

            case EXCLUDE:
                excludePath(path);
                break;

            case SKIP:
                break;
        }
        return Optional.empty();
    }

    private Optional<GameInfo> selectNewMetacriticName(Path path,
                                                       String name,
                                                       GamePlatform platform,
                                                       ChoiceProvider choiceProvider) throws Exception {
        final Optional<String> newName = choiceProvider.selectNewName(path, name, platform);
        if (newName.isPresent()) {
            return getMetacriticGameInfo(path, newName.get(), platform, choiceProvider);
        } else {
            return Optional.empty();
        }
    }

    private Optional<GameInfo> selectNewGiantBombName(Path path,
                                                      String name,
                                                      GamePlatform platform,
                                                      ChoiceProvider choiceProvider) throws Exception {
        final Optional<String> newName = choiceProvider.selectNewName(path, name, platform);
        if (newName.isPresent()) {
            return getGiantBombGameInfo(path, newName.get(), platform, choiceProvider);
        } else {
            return Optional.empty();
        }
    }

    private Optional<GameInfo> handleMultipleMetacriticSearchResults(Path path,
                                                                     String name,
                                                                     GamePlatform platform,
                                                                     List<GameRawBriefInfo> briefInfos,
                                                                     ChoiceProvider choiceProvider) throws Exception {
        final MultipleSearchResultsChoice choice = choiceProvider.getMultipleMetacriticSearchResultsChoice(path, name, platform, briefInfos);
        switch (choice) {
            case CHOOSE:
                return chooseFromMultipleMetacriticSearchResults(path, name, platform, briefInfos, choiceProvider);

            case NEW_NAME:
                return selectNewMetacriticName(path, name, platform, choiceProvider);

            case EXCLUDE:
                excludePath(path);
                break;

            case SUB_LIBRARY:
                designateSubLibrary(path, name, platform, choiceProvider);
                break;

            case SKIP:
                break;
        }
        return Optional.empty();
    }

    private Optional<GameInfo> chooseFromMultipleMetacriticSearchResults(Path path,
                                                                         String name,
                                                                         GamePlatform platform,
                                                                         List<GameRawBriefInfo> briefInfos,
                                                                         ChoiceProvider choiceProvider) throws Exception {
        final Optional<GameRawBriefInfo> choice = choiceProvider.chooseFromMultipleResults(path, name, platform, briefInfos);
        if (choice.isPresent()) {
            return fetchMetacriticGameInfo(choice.get(), platform);
        } else {
            return Optional.empty();
        }
    }

    private Optional<GameInfo> handleMultipleGiantBombSearchResults(Path path,
                                                                    String name,
                                                                    GamePlatform platform,
                                                                    List<GameRawBriefInfo> briefInfos,
                                                                    ChoiceProvider choiceProvider) throws Exception {
        final MultipleSearchResultsChoice choice = choiceProvider.getMultipleGiantBombSearchResultsChoice(path, name, platform, briefInfos);
        switch (choice) {
            case CHOOSE:
                return chooseFromMultipleGiantBombSearchResults(path, name, platform, briefInfos, choiceProvider);

            case NEW_NAME:
                return selectNewGiantBombName(path, name, platform, choiceProvider);

            case SKIP:
                break;
        }
        return Optional.empty();
    }

    private Optional<GameInfo> chooseFromMultipleGiantBombSearchResults(Path path,
                                                                        String name,
                                                                        GamePlatform platform,
                                                                        List<GameRawBriefInfo> briefInfos,
                                                                        ChoiceProvider choiceProvider) throws Exception {
        final Optional<GameRawBriefInfo> choice = choiceProvider.chooseFromMultipleResults(path, name, platform, briefInfos);
        if (choice.isPresent()) {
            return fetchGiantBombGameInfo(choice.get(), platform);
        } else {
            return Optional.empty();
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

    // FIXME: While fetching, set a boolean flag to true and display an indeterminate loading progress indicator.
    private Optional<GameInfo> fetchMetacriticGameInfo(GameRawBriefInfo briefInfo, GamePlatform platform) throws Exception {
        LOG.debug("Getting Metacritic gameInfo from brief: {}", briefInfo);
        Platform.runLater(() -> message.setValue(String.format("Fetching game info: '%s'...", briefInfo.getName())));

        final GameInfo gameInfo = metacriticInfoService.getGameInfo(briefInfo.getName(), platform).orElseThrow(
            () -> new IndexterException("Specific Metacritic search found nothing: %s", briefInfo)
        );
        return Optional.of(gameInfo);
    }

    // FIXME: While fetching, set a boolean flag to true and display an indeterminate loading progress indicator.
    private Optional<GameInfo> fetchGiantBombGameInfo(GameRawBriefInfo briefInfo, GamePlatform platform) throws Exception {
        LOG.debug("Getting GiantBomb gameInfo from brief: {}", briefInfo);
        Platform.runLater(() -> message.setValue(String.format("Fetching game info: '%s'...", briefInfo.getName())));

        final GameInfo gameInfo = giantBombInfoService.getGameInfo(briefInfo.getGiantBombApiDetailUrl().get(), platform).orElseThrow(
            () -> new IndexterException("Specific GiantBomb search found nothing: %s", briefInfo)
        );
        return Optional.of(gameInfo);
    }

    private void excludePath(Path path) {
        info("Excluding: %s", path);
        libraryManager.setExcluded(path);
    }

    private GameInfo mergeGameInfo(GameInfo metacriticGameInfo, GameInfo giantBombGameInfo) {
        final List<String> genres = new ArrayList<>(giantBombGameInfo.getGenres());
        genres.addAll(metacriticGameInfo.getGenres());

        return new GameInfo(
            metacriticGameInfo.getName(),
            metacriticGameInfo.getPlatform(),
            Optionals.or(metacriticGameInfo.getDescription(), giantBombGameInfo.getDescription()),
            Optionals.or(metacriticGameInfo.getReleaseDate(), giantBombGameInfo.getReleaseDate()),
            metacriticGameInfo.getCriticScore(),
            metacriticGameInfo.getUserScore(),
            genres,
            giantBombGameInfo.getGiantBombApiDetailsUrl(),
            Optionals.or(giantBombGameInfo.getThumbnailData(), metacriticGameInfo.getThumbnailData()),
            Optionals.or(giantBombGameInfo.getPosterData(), metacriticGameInfo.getPosterData())
        );
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
