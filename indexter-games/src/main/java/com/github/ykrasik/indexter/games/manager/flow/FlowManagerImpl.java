package com.github.ykrasik.indexter.games.manager.flow;

import com.github.ykrasik.indexter.AbstractService;
import com.github.ykrasik.indexter.exception.IndexterException;
import com.github.ykrasik.indexter.games.datamodel.GamePlatform;
import com.github.ykrasik.indexter.games.datamodel.info.GameInfo;
import com.github.ykrasik.indexter.games.datamodel.info.giantbomb.GiantBombGameInfo;
import com.github.ykrasik.indexter.games.datamodel.info.giantbomb.GiantBombSearchResult;
import com.github.ykrasik.indexter.games.datamodel.info.metacritic.MetacriticGameInfo;
import com.github.ykrasik.indexter.games.datamodel.info.metacritic.MetacriticSearchResult;
import com.github.ykrasik.indexter.games.datamodel.persistence.Game;
import com.github.ykrasik.indexter.games.datamodel.persistence.Library;
import com.github.ykrasik.indexter.games.info.giantbomb.GiantBombGameInfoService;
import com.github.ykrasik.indexter.games.info.metacritic.MetacriticGameInfoService;
import com.github.ykrasik.indexter.games.manager.flow.choice.ChoiceProvider;
import com.github.ykrasik.indexter.games.manager.flow.choice.type.Choice;
import com.github.ykrasik.indexter.games.manager.flow.choice.type.ChoiceData;
import com.github.ykrasik.indexter.games.manager.game.GameManager;
import com.github.ykrasik.indexter.games.manager.library.LibraryManager;
import com.github.ykrasik.indexter.util.FileUtils;
import com.github.ykrasik.indexter.util.PlatformUtils;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.control.Button;
import org.controlsfx.control.StatusBar;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
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
    private final MetacriticGameInfoService metacriticInfoService;
    private final GiantBombGameInfoService giantBombInfoService;
    private final ChoiceProvider choiceProvider;

    private final Button stopRefreshButton = new Button("Stop");
    private final StatusBar statusBar = new StatusBar();

    private ExecutorService executorService;

    public FlowManagerImpl(LibraryManager libraryManager,
                           GameManager gameManager,
                           MetacriticGameInfoService metacriticInfoService,
                           GiantBombGameInfoService giantBombInfoService,
                           ChoiceProvider choiceProvider) {
        this.libraryManager = Objects.requireNonNull(libraryManager);
        this.gameManager = Objects.requireNonNull(gameManager);
        this.metacriticInfoService = Objects.requireNonNull(metacriticInfoService);
        this.giantBombInfoService = Objects.requireNonNull(giantBombInfoService);
        this.choiceProvider = Objects.requireNonNull(choiceProvider);

        statusBar.setText("Welcome to inDexter!");
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

        final List<Library> libraries = libraryManager.getAllLibraries();
        final int total = libraries.size();
        for (int i = 0; i < total; i++) {
            setProgress(i, total);
            final Library library = libraries.get(i);
            refreshLibrary(library);
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

        final List<Game> obsoleteGames = new ArrayList<>();
        final ObservableList<Game> games = gameManager.getAllGames();
        for (int i = 0; i < games.size(); i++) {
            setProgress(i, games.size());
            final Game game = games.get(i);
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
    public void processPath(Library library, Path path, ExceptionHandler exceptionHandler) {
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

    private void refreshLibrary(Library library) throws Exception {
        info("Refreshing library: '%s'", library);

        final List<Path> directories = FileUtils.listChildDirectories(library.getPath());
        final int total = directories.size();
        for (int i = 0; i < total; i++) {
            setProgress(i, total);
            final Path path = directories.get(i);
            doProcessPath(library, path);
        }

        info("Finished refreshing library: '%s'", library);
    }

    private void doProcessPath(Library library, Path path) throws Exception {
        info("Processing path: {}...", path);

        if (libraryManager.isLibrary(path)) {
            info("{} is a library, skipping...", path);
            return;
        }

        // TODO: Excludes should belong to their own manager.
        if (libraryManager.isExcluded(path)) {
            info("{} is excluded, skipping...", path);
            return;
        }

        if (gameManager.isPathMapped(path)) {
            info("{} is already mapped, skipping...", path);
            return;
        }

        if (tryCreateLibrary(path, library.getPlatform())) {
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

        final Library library = libraryManager.createLibrary(libraryName.get(), path, platform);
        info("New library created: %s", library);
        refreshLibrary(library);
        return true;
    }

    private String getName(Path path) {
        // Remove all metaData enclosed with '[]' from the game name.
        final String rawName = path.getFileName().toString();
        return META_DATA_PATTERN.matcher(rawName).replaceAll("");
    }

    // FIXME: There should be an explicit "skip" button. Any other form of cancellation should show the prev screen.
    private void addPath(Library library, Path path, String name) throws Exception {
        final GamePlatform platform = library.getPlatform();
        LOG.debug("addPath: path={}, name='{}', platform={}", path, name, platform);

        final Optional<MetacriticGameInfo> metacriticGameOptional = getMetacriticGame(path, name, platform);
        if (metacriticGameOptional.isPresent()) {
            final MetacriticGameInfo metacriticGameInfo = metacriticGameOptional.get();
            LOG.debug("Metacritic gameInfo: {}", metacriticGameInfo);

            // FIXME: This is not good enough, giantbomb should be able to cancel the process too.
            // FIXME: Use exceptions?
            final Optional<GiantBombGameInfo> giantBombGameOptional = getGiantBombGame(path, metacriticGameInfo.getName(), platform);
            final GameInfo gameInfo;
            if (giantBombGameOptional.isPresent()) {
                final GiantBombGameInfo giantBombGameInfo = giantBombGameOptional.get();
                LOG.debug("GiantBomb gameInfo: {}", giantBombGameInfo);
                gameInfo = GameInfo.merge(metacriticGameInfo, giantBombGameInfo);
            } else {
                LOG.debug("GiantBomb gameInfo not found.");
                gameInfo = GameInfo.from(metacriticGameInfo);
            }

            final Game game = gameManager.addGame(gameInfo, path, platform);
            libraryManager.addGameToLibrary(game, library);
        }
    }

    private Optional<MetacriticGameInfo> getMetacriticGame(Path path, String name, GamePlatform platform) throws Exception {
        info("Searching Metacritic for '%s'[%s]...", name, platform);
        final List<MetacriticSearchResult> searchResults = metacriticInfoService.searchGames(name, platform);
        info("Metacritic Search: '%s'[%s] found %d results.", name, platform, searchResults.size());

        if (searchResults.isEmpty()) {
            return handleNoMetacriticSearchResults(path, name, platform);
        }

        if (searchResults.size() > 1) {
            return handleMultipleMetacriticSearchResults(path, name, platform, searchResults);
        }

        final MetacriticSearchResult singleSearchResult = searchResults.get(0);
        return Optional.of(fetchMetacriticGameInfo(singleSearchResult, platform));
    }

    private Optional<GiantBombGameInfo> getGiantBombGame(Path path, String name, GamePlatform platform) throws Exception {
        info("Searching GiantBomb for '%s'[%s]...", name, platform);
        final List<GiantBombSearchResult> searchResults = giantBombInfoService.searchGames(name, platform);
        info("GiantBomb Search: '%s'[%s] found %d results.", name, platform, searchResults.size());

        if (searchResults.isEmpty()) {
            return handleNoGiantBombSearchResults(path, name, platform);
        }

        if (searchResults.size() > 1) {
            return handleMultipleGiantBombSearchResults(path, name, platform, searchResults);
        }

        final GiantBombSearchResult singleSearchResult = searchResults.get(0);
        return Optional.of(fetchGiantBombGameInfo(singleSearchResult, platform));
    }

    private Optional<MetacriticGameInfo> handleNoMetacriticSearchResults(Path path, String name, GamePlatform platform) throws Exception {
        final Choice choice = choiceProvider.onNoMetacriticSearchResults(name, platform, path);
        switch (choice.getType()) {
            case NEW_NAME:
                final String newName = (String) ((ChoiceData) choice).getChoice();
                return getMetacriticGame(path, newName, platform);
            case EXCLUDE: excludePath(path); break;
            default: break;
        }
        return Optional.empty();
    }

    private Optional<GiantBombGameInfo> handleNoGiantBombSearchResults(Path path, String name, GamePlatform platform) throws Exception {
        final Choice choice = choiceProvider.onNoGiantBombSearchResults(name, platform, path);
        switch (choice.getType()) {
            case NEW_NAME:
                final String newName = (String) ((ChoiceData) choice).getChoice();
                return getGiantBombGame(path, newName, platform);
            case EXCLUDE: excludePath(path); break;
            default: break;
        }
        return Optional.empty();
    }

    private Optional<MetacriticGameInfo> handleMultipleMetacriticSearchResults(Path path,
                                                                               String name,
                                                                               GamePlatform platform,
                                                                               List<MetacriticSearchResult> searchResults) throws Exception {
        final Choice choice = choiceProvider.onMultipleMetacriticSearchResults(name, platform, path, searchResults);
        switch (choice.getType()) {
            case CHOOSE:
                final MetacriticSearchResult searchResult = (MetacriticSearchResult) ((ChoiceData) choice).getChoice();
                return Optional.of(fetchMetacriticGameInfo(searchResult, platform));

            case NEW_NAME:
                final String newName = (String) ((ChoiceData) choice).getChoice();
                return getMetacriticGame(path, newName, platform);

            case EXCLUDE: excludePath(path); break;
            default: break;
        }
        return Optional.empty();
    }

    private Optional<GiantBombGameInfo> handleMultipleGiantBombSearchResults(Path path,
                                                                             String name,
                                                                             GamePlatform platform,
                                                                             List<GiantBombSearchResult> searchResults) throws Exception {
        final Choice choice = choiceProvider.onMultipleGiantBombSearchResults(name, platform, path, searchResults);
        switch (choice.getType()) {
            case CHOOSE:
                final GiantBombSearchResult searchResult = (GiantBombSearchResult) ((ChoiceData) choice).getChoice();
                return Optional.of(fetchGiantBombGameInfo(searchResult, platform));

            case NEW_NAME:
                final String newName = (String) ((ChoiceData) choice).getChoice();
                return getGiantBombGame(path, newName, platform);

            default: break;
        }
        return Optional.empty();
    }

    // FIXME: While fetching, set a boolean flag to true and display an indeterminate loading progress indicator.
    private MetacriticGameInfo fetchMetacriticGameInfo(MetacriticSearchResult searchResult, GamePlatform platform) throws Exception {
        info("Fetching game from Metacritic: '%s'[%s]", searchResult.getName(), platform);
        return metacriticInfoService.getGameInfo(searchResult.getName(), platform).orElseThrow(
            () -> new IndexterException("Specific Metacritic search found nothing: %s", searchResult)
        );
    }

    // FIXME: While fetching, set a boolean flag to true and display an indeterminate loading progress indicator.
    private GiantBombGameInfo fetchGiantBombGameInfo(GiantBombSearchResult searchResult, GamePlatform platform) throws Exception {
        info("Fetching game from GiantBomb: '%s'[%s]", searchResult.getName(), platform);
        return giantBombInfoService.getGameInfo(searchResult.getApiDetailUrl()).orElseThrow(
            () -> new IndexterException("Specific GiantBomb search found nothing: %s", searchResult)
        );
    }

    private void excludePath(Path path) {
        info("Excluding: %s", path);
        libraryManager.setExcluded(path);
    }

    private void info(String format, Object... args) {
        final String message = String.format(format, args);
        LOG.info(message);
        setMessage(message);
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
            info("Cancelled");
        });
    }

    private void setProgress(int current, int total) {
        final double progress = (double) current / total;
        PlatformUtils.runLater(() -> statusBar.setProgress(progress));
    }

    private void setMessage(String message) {
        PlatformUtils.runLater(() -> statusBar.setText(message));
    }
}
