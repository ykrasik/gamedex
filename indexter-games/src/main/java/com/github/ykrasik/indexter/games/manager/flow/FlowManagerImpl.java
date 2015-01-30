package com.github.ykrasik.indexter.games.manager.flow;

import com.github.ykrasik.indexter.AbstractService;
import com.github.ykrasik.indexter.exception.IndexterException;
import com.github.ykrasik.indexter.games.datamodel.*;
import com.github.ykrasik.indexter.games.info.GameInfoService;
import com.github.ykrasik.indexter.games.info.GameRawBriefInfo;
import com.github.ykrasik.indexter.games.manager.flow.choice.ChoiceProvider;
import com.github.ykrasik.indexter.games.manager.flow.choice.MultipleSearchResultsChoice;
import com.github.ykrasik.indexter.games.manager.flow.choice.NoSearchResultsChoice;
import com.github.ykrasik.indexter.games.manager.game.GameManager;
import com.github.ykrasik.indexter.games.manager.library.LibraryManager;
import com.github.ykrasik.indexter.util.FileUtils;
import com.github.ykrasik.indexter.util.Optionals;
import com.github.ykrasik.indexter.util.PlatformUtils;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.control.Button;
import org.controlsfx.control.StatusBar;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

/**
 * @author Yevgeny Krasik
 */
public class FlowManagerImpl extends AbstractService implements FlowManager {
    private static final Pattern META_DATA_PATTERN = Pattern.compile("(\\[.*?\\])|(-)");

    // FIXME: Log everything to a textual logger.

    private final LibraryManager libraryManager;
    private final GameManager gameManager;
    private final GameInfoService metacriticInfoService;
    private final GameInfoService giantBombInfoService;
    private final ChoiceProvider choiceProvider;

    private final StringProperty currentLibrary = new SimpleStringProperty();
    private final StringProperty currentPath = new SimpleStringProperty();
    private final StringProperty message = new SimpleStringProperty();
    private final DoubleProperty refreshLibrariesProgress = new SimpleDoubleProperty();
    private final DoubleProperty refreshLibraryProgress = new SimpleDoubleProperty();

    private final Button stopRefreshButton = new Button("Stop");
    private final StatusBar statusBar = new StatusBar();

    private ExecutorService executorService;

    public FlowManagerImpl(LibraryManager libraryManager,
                           GameManager gameManager,
                           GameInfoService metacriticInfoService,
                           GameInfoService giantBombInfoService,
                           ChoiceProvider choiceProvider) {
        this.libraryManager = Objects.requireNonNull(libraryManager);
        this.gameManager = Objects.requireNonNull(gameManager);
        this.metacriticInfoService = Objects.requireNonNull(metacriticInfoService);
        this.giantBombInfoService = Objects.requireNonNull(giantBombInfoService);
        this.choiceProvider = Objects.requireNonNull(choiceProvider);

        statusBar.setText("Welcome to inDexter!");
        statusBar.textProperty().bind(message);
        statusBar.progressProperty().bind(refreshLibraryProgress);
        statusBar.getRightItems().add(stopRefreshButton);

        stopRefreshButton.setDisable(true);
        stopRefreshButton.setCancelButton(true);
    }

    @Override
    protected void doStart() throws Exception {
        executorService = Executors.newSingleThreadExecutor();
    }

    @Override
    protected void doStop() throws Exception {
        executorService.shutdownNow();
    }

    @Override
    public void refreshLibraries(ExceptionHandler exceptionHandler) {
        submit(new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                doRefreshLibraries();
                return null;
            }
        }, exceptionHandler);
    }

    private void doRefreshLibraries() throws Exception {
        info("Refreshing libraries...");

        PlatformUtils.runLater(() -> refreshLibrariesProgress.setValue(0));
        final List<LocalLibrary> libraries = libraryManager.getAllLibraries();
        final int total = libraries.size();
        for (int i = 0; i < total; i++) {
            final LocalLibrary library = libraries.get(i);
            refreshLibrary(library);
            final double current = i;
            PlatformUtils.runLater(() -> refreshLibrariesProgress.setValue(current / total));
        }

        info("Finished refreshing libraries.");
    }

    @Override
    public void cleanupGames(ExceptionHandler exceptionHandler) {
        submit(new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                doCleanupGames();
                return null;
            }
        }, exceptionHandler);
    }

    private void doCleanupGames() {
        info("Cleaning up games...");

        final List<LocalGame> obsoleteGames = new ArrayList<>();
        final ObservableList<LocalGame> games = gameManager.getAllGames();
        for (int i = 0; i < games.size(); i++) {
            setProgress(i, games.size());
            final LocalGame game = games.get(i);
            final Path path = game.getPath();
            if (!Files.exists(path)) {
                info("Obsolete path detected: %s", path);
                obsoleteGames.add(game);
            }
        }

        info("Detected %d obsolete games.", obsoleteGames.size());
        obsoleteGames.forEach(gameManager::deleteGame);
        info("Removed %d obsolete games.", obsoleteGames.size());
    }

    @Override
    public void processPath(LocalLibrary library, Path path, ExceptionHandler exceptionHandler) {
        submit(new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                doProcessPath(library, path);
                return null;
            }
        }, exceptionHandler);
    }

    @Override
    public StatusBar getStatusBar() {
        return statusBar;
    }

    private void refreshLibrary(LocalLibrary localLibrary) throws Exception {
        final Library library = localLibrary.getLibrary();
        info("Refreshing library: '%s'", library);
        PlatformUtils.runLater(() -> currentLibrary.setValue(library.getName()));

        PlatformUtils.runLater(() -> refreshLibraryProgress.setValue(0));
        final List<Path> directories = FileUtils.listChildDirectories(library.getPath());
        final int total = directories.size();
        for (int i = 0; i < total; i++) {
            final Path path = directories.get(i);
            doProcessPath(localLibrary, path);
            final double current = i;
            PlatformUtils.runLater(() -> refreshLibraryProgress.setValue(current / total));
        }

        info("Finished refreshing library: '%s'", localLibrary);
    }

    private void doProcessPath(LocalLibrary library, Path path) throws Exception {
        LOG.debug("Processing path: {}...", path);
        PlatformUtils.runLater(() -> currentPath.setValue(path.toString()));

        if (libraryManager.isLibrary(path)) {
            LOG.debug("{} is a library, skipping...", path);
            return;
        }

        // TODO: Excludes should belong to their own manager.
        if (libraryManager.isExcluded(path)) {
            LOG.debug("{} is excluded, skipping...", path);
            return;
        }

        if (gameManager.isPathMapped(path)) {
            LOG.debug("{} is already mapped, skipping...", path);
            return;
        }

        if (tryCreateLibrary(path, library.getLibrary().getPlatform())) {
            return;
        }

        final String name = getName(path);
        addPath(library, path, name);
        LOG.debug("Finished Processing {}.", path);
    }

    private boolean tryCreateLibrary(Path path, GamePlatform platform) throws Exception {
        if (!FileUtils.hasChildDirectories(path)) {
            // Only directories that have sub-directories can be libraries.
            return false;
        }

        if (!choiceProvider.shouldCreateLibrary(path)) {
            return false;
        }

        final Optional<String> libraryName = choiceProvider.getLibraryName(path, platform);
        if (!libraryName.isPresent()) {
            return false;
        }

        final Library newLibrary = new Library(libraryName.get(), path, platform);
        final LocalLibrary localLibrary = libraryManager.addLibrary(newLibrary);
        info("New library created: %s", newLibrary);
        refreshLibrary(localLibrary);
        return true;
    }

    private String getName(Path path) {
        // Remove all metaData enclosed with '[]' from the game name.
        final String rawName = path.getFileName().toString();
        return META_DATA_PATTERN.matcher(rawName).replaceAll("");
    }

    // FIXME: There should be an explicit "skip" button. Any other form of cancellation should show the prev screen.
    private void addPath(LocalLibrary library, Path path, String name) throws Exception {
        final GamePlatform platform = library.getLibrary().getPlatform();
        LOG.debug("addPath: path={}, name='{}', platform={}", path, name, platform);

        final Optional<Game> metacriticGameOptional = getMetacriticGame(path, name, platform);
        if (metacriticGameOptional.isPresent()) {
            final Game metacriticGame = metacriticGameOptional.get();
            LOG.debug("Metacritic gameInfo: {}", metacriticGame);
            final Optional<Game> giantBombGameOptional = getGiantBombGame(path, metacriticGame.getName(), platform);
            final Game game;
            if (giantBombGameOptional.isPresent()) {
                final Game giantBombGame = giantBombGameOptional.get();
                LOG.debug("GiantBomb gameInfo: {}", giantBombGame);
                game = mergeGameInfo(metacriticGame, giantBombGame);
            } else {
                LOG.debug("GiantBomb gameInfo not found.");
                game = metacriticGame;
            }

            final LocalGame localGame = gameManager.addGame(game, path);
            libraryManager.addGameToLibrary(localGame, library);
        }
    }

    private Optional<Game> getMetacriticGame(Path path, String name, GamePlatform platform) throws Exception {
        debug("Searching Metacritic for '%s'[%s]...", name, platform);
        final List<GameRawBriefInfo> briefInfos = metacriticInfoService.searchGames(name, platform);
        debug("Metacritic Search: '%s'[%s] found %d results.", name, platform, briefInfos.size());

        if (briefInfos.isEmpty()) {
            return handleNoMetacriticSearchResults(path, name, platform);
        }

        if (briefInfos.size() > 1) {
            return handleMultipleMetacriticSearchResults(path, name, platform, briefInfos);
        }

        final GameRawBriefInfo briefInfo = briefInfos.get(0);
        return Optional.of(fetchMetacriticGameInfo(briefInfo, platform));
    }

    private Optional<Game> getGiantBombGame(Path path, String name, GamePlatform platform) throws Exception {
        debug("Searching GiantBomb for '%s'[%s]...", name, platform);
        final List<GameRawBriefInfo> briefInfos = giantBombInfoService.searchGames(name, platform);
        debug("GiantBomb Search: '%s'[%s] found %d results.", name, platform, briefInfos.size());

        if (briefInfos.isEmpty()) {
            return selectNewGiantBombName(path, name, platform);
        }

        if (briefInfos.size() > 1) {
            return handleMultipleGiantBombSearchResults(path, name, platform, briefInfos);
        }

        final GameRawBriefInfo briefInfo = briefInfos.get(0);
        return Optional.of(fetchGiantBombGameInfo(briefInfo, platform));
    }

    private Optional<Game> handleNoMetacriticSearchResults(Path path, String name, GamePlatform platform) throws Exception {
        final NoSearchResultsChoice choice = choiceProvider.getNoMetacriticSearchResultsChoice(path, name, platform);
        switch (choice) {
            case NEW_NAME:
                return selectNewMetacriticName(path, name, platform);

            case EXCLUDE:
                excludePath(path);
                break;

            case SKIP:
                break;
        }
        return Optional.empty();
    }

    private Optional<Game> selectNewMetacriticName(Path path, String name, GamePlatform platform) throws Exception {
        final Optional<String> newName = choiceProvider.selectNewName(path, name, platform);
        return Optionals.flatMap(newName, nameChoice -> getMetacriticGame(path, nameChoice, platform));
    }

    private Optional<Game> selectNewGiantBombName(Path path, String name, GamePlatform platform) throws Exception {
        final Optional<String> newName = choiceProvider.selectNewName(path, name, platform);
        return Optionals.flatMap(newName, nameChoice -> getGiantBombGame(path, nameChoice, platform));
    }

    private Optional<Game> handleMultipleMetacriticSearchResults(Path path,
                                                                 String name,
                                                                 GamePlatform platform,
                                                                 List<GameRawBriefInfo> briefInfos) throws Exception {
        final MultipleSearchResultsChoice choice = choiceProvider.getMultipleMetacriticSearchResultsChoice(path, name, platform, briefInfos);
        switch (choice) {
            case CHOOSE:
                return chooseFromMultipleMetacriticSearchResults(path, name, platform, briefInfos);

            case NEW_NAME:
                return selectNewMetacriticName(path, name, platform);

            case EXCLUDE:
                excludePath(path);
                break;

            case SKIP:
                break;
        }
        return Optional.empty();
    }

    private Optional<Game> chooseFromMultipleMetacriticSearchResults(Path path,
                                                                     String name,
                                                                     GamePlatform platform,
                                                                     List<GameRawBriefInfo> briefInfos) throws Exception {
        final Optional<GameRawBriefInfo> choice = choiceProvider.chooseFromMultipleResults(path, name, platform, briefInfos);
        return Optionals.map(choice, chosen -> fetchMetacriticGameInfo(chosen, platform));
    }

    private Optional<Game> handleMultipleGiantBombSearchResults(Path path,
                                                                String name,
                                                                GamePlatform platform,
                                                                List<GameRawBriefInfo> briefInfos) throws Exception {
        final MultipleSearchResultsChoice choice = choiceProvider.getMultipleGiantBombSearchResultsChoice(path, name, platform, briefInfos);
        switch (choice) {
            case CHOOSE:
                return chooseFromMultipleGiantBombSearchResults(path, name, platform, briefInfos);

            case NEW_NAME:
                return selectNewGiantBombName(path, name, platform);

            default:
                break;
        }
        return Optional.empty();
    }

    private Optional<Game> chooseFromMultipleGiantBombSearchResults(Path path,
                                                                    String name,
                                                                    GamePlatform platform,
                                                                    List<GameRawBriefInfo> briefInfos) throws Exception {
        final Optional<GameRawBriefInfo> choice = choiceProvider.chooseFromMultipleResults(path, name, platform, briefInfos);
        return Optionals.map(choice, chosen -> fetchGiantBombGameInfo(chosen, platform));
    }

    // FIXME: While fetching, set a boolean flag to true and display an indeterminate loading progress indicator.
    private Game fetchMetacriticGameInfo(GameRawBriefInfo briefInfo, GamePlatform platform) throws Exception {
        LOG.debug("Getting Metacritic gameInfo from brief: {}", briefInfo);
        PlatformUtils.runLater(() -> message.setValue(String.format("Fetching game info: '%s'...", briefInfo.getName())));

        return metacriticInfoService.getGameInfo(briefInfo.getName(), platform).orElseThrow(
            () -> new IndexterException("Specific Metacritic search found nothing: %s", briefInfo)
        );
    }

    // FIXME: While fetching, set a boolean flag to true and display an indeterminate loading progress indicator.
    private Game fetchGiantBombGameInfo(GameRawBriefInfo briefInfo, GamePlatform platform) throws Exception {
        LOG.debug("Getting GiantBomb gameInfo from brief: {}", briefInfo);
        PlatformUtils.runLater(() -> message.setValue(String.format("Fetching game info: '%s'...", briefInfo.getName())));

        return giantBombInfoService.getGameInfo(briefInfo.getGiantBombApiDetailUrl().get(), platform).orElseThrow(
            () -> new IndexterException("Specific GiantBomb search found nothing: %s", briefInfo)
        );
    }

    private void excludePath(Path path) {
        info("Excluding: %s", path);
        libraryManager.setExcluded(path);
    }

    private Game mergeGameInfo(Game metacriticGame, Game giantBombGame) {
        final List<String> genres = new ArrayList<>(giantBombGame.getGenres());
        genres.addAll(metacriticGame.getGenres());

        return new Game(
            metacriticGame.getName(),
            metacriticGame.getPlatform(),
            Optionals.or(giantBombGame.getDescription(), metacriticGame.getDescription()),
            Optionals.or(metacriticGame.getReleaseDate(), giantBombGame.getReleaseDate()),
            metacriticGame.getCriticScore(),
            metacriticGame.getUserScore(),
            genres,
            giantBombGame.getGiantBombApiDetailsUrl(),
            Optionals.or(giantBombGame.getThumbnailData(), metacriticGame.getThumbnailData()),
            Optionals.or(giantBombGame.getPosterData(), metacriticGame.getPosterData())
        );
    }

    private void info(String format, Object... args) {
        final String message = String.format(format, args);
        LOG.info(message);
        PlatformUtils.runLater(() -> this.message.setValue(message));
    }

    private void debug(String format, Object... args) {
        final String message = String.format(format, args);
        LOG.debug(message);
        PlatformUtils.runLater(() -> this.message.setValue(message));
    }

    private void submit(Task<?> task, ExceptionHandler exceptionHandler) {
        task.setOnFailed(event -> exceptionHandler.handle(task.getException()));
        linkStopButton(task);
        executorService.submit(task);
    }

    private void linkStopButton(Task<?> task) {
        stopRefreshButton.disableProperty().bind(task.runningProperty().not());
        stopRefreshButton.setOnAction(e -> {
            task.cancel();
            message.set("Cancelled");
        });
    }

    private void setProgress(int current, int total) {
        PlatformUtils.runLater(() -> refreshLibraryProgress.setValue((double) current / total));
    }
}
