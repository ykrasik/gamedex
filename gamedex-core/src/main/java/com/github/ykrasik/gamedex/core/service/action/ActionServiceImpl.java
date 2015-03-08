package com.github.ykrasik.gamedex.core.service.action;

import com.github.ykrasik.gamedex.common.exception.GameDexException;
import com.github.ykrasik.gamedex.common.exception.RunnableThrows;
import com.github.ykrasik.gamedex.common.util.FileUtils;
import com.github.ykrasik.gamedex.core.config.ConfigService;
import com.github.ykrasik.gamedex.core.config.ConfigType;
import com.github.ykrasik.gamedex.core.javafx.property.ThreadAwareBooleanProperty;
import com.github.ykrasik.gamedex.core.javafx.property.ThreadAwareDoubleProperty;
import com.github.ykrasik.gamedex.core.javafx.property.ThreadAwareStringProperty;
import com.github.ykrasik.gamedex.core.manager.exclude.ExcludedPathManager;
import com.github.ykrasik.gamedex.core.manager.game.GameManager;
import com.github.ykrasik.gamedex.core.manager.info.GameInfoProviderManager;
import com.github.ykrasik.gamedex.core.manager.info.SearchContext;
import com.github.ykrasik.gamedex.core.manager.library.LibraryManager;
import com.github.ykrasik.gamedex.core.service.dialog.DialogService;
import com.github.ykrasik.gamedex.core.service.screen.ScreenService;
import com.github.ykrasik.gamedex.core.service.task.TaskService;
import com.github.ykrasik.gamedex.core.ui.library.LibraryDef;
import com.github.ykrasik.gamedex.datamodel.GamePlatform;
import com.github.ykrasik.gamedex.datamodel.flow.LibraryHierarchy;
import com.github.ykrasik.gamedex.datamodel.persistence.ExcludedPath;
import com.github.ykrasik.gamedex.datamodel.persistence.Game;
import com.github.ykrasik.gamedex.datamodel.persistence.Library;
import com.github.ykrasik.gamedex.datamodel.provider.GameInfo;
import com.github.ykrasik.gamedex.datamodel.provider.UnifiedGameInfo;
import com.github.ykrasik.opt.Opt;
import com.gs.collections.api.list.ImmutableList;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @author Yevgeny Krasik
 */
@Slf4j
@Accessors(fluent = true)
@RequiredArgsConstructor
public class ActionServiceImpl implements ActionService {
    private static final Pattern META_DATA_PATTERN = Pattern.compile("(\\[.*?\\])|(-)");
    private static final Pattern SPACE_PATTERN = Pattern.compile("\\s+");

    private final StringProperty messageProperty = new ThreadAwareStringProperty();
    private final DoubleProperty progressProperty = new ThreadAwareDoubleProperty();
    private final BooleanProperty fetchingProperty = new ThreadAwareBooleanProperty();

    @NonNull private final ConfigService configService;
    @NonNull private final TaskService taskService;
    @NonNull private final ScreenService screenService;
    @NonNull private final DialogService dialogService;
    @NonNull private final GameManager gameManager;
    @NonNull private final LibraryManager libraryManager;
    @NonNull private final ExcludedPathManager excludedPathManager;
    @NonNull private final GameInfoProviderManager metacriticManager;
    @NonNull private final GameInfoProviderManager giantBombManager;

    @Override
    public ReadOnlyStringProperty messageProperty() {
        return messageProperty;
    }

    @Override
    public ReadOnlyDoubleProperty progressProperty() {
        return progressProperty;
    }

    @Override
    public ReadOnlyBooleanProperty fetchProgressProperty() {
        return fetchingProperty;
    }

    @Override
    public void stopTask(Task<Void> task) {
        log.info("Cancelled.");
        messageProperty.unbind();
        fetchingProperty.unbind();
        message("Cancelled.");
        progressProperty.setValue(0.0);
        fetchingProperty.setValue(false);
    }

    // TODO: Do this on the background thread and return a task?
    @Override
    public void addNewLibrary() {
        try {
            final Opt<LibraryDef> libraryDefOpt = dialogService.addLibraryDialog(prevDirectoryProperty().get());
            if (libraryDefOpt.isPresent()) {
                final LibraryDef libraryDef = libraryDefOpt.get();
                prevDirectoryProperty().set(Opt.of(libraryDef.getPath()));
                createLibraryFromDef(libraryDef);
            }
        } catch (Exception e) {
            dialogService.showException(e);
        }
    }

    @Override
    public Task<Void> refreshLibraries() {
        return taskService.submit(this::doRefreshLibraries);
    }

    private void doRefreshLibraries() throws Exception {
        message("Refreshing libraries...");

        final List<Library> libraries = libraryManager.getAllLibraries();
        for (Library library : libraries) {
            checkStopped();

            final LibraryHierarchy libraryHierarchy = new LibraryHierarchy(library);
            refreshCurrentLibrary(libraryHierarchy);
        }

        message("Finished refreshing libraries.");
    }

    @Override
    public Task<Void> cleanupGames() {
        return taskService.submit(this::doCleanupGames);
    }

    // TODO: Make this a total cleanup? libraries, excluded, evertyhing?
    private void doCleanupGames() {
        message("Cleaning up games...");

        final List<Game> obsoleteGames = new ArrayList<>();
        final List<Game> games = gameManager.getAllGames();
        for (int i = 0; i < games.size(); i++) {
            checkStopped();

            setProgress(i, games.size());
            final Game game = games.get(i);
            final Path path = game.getPath();
            if (!Files.exists(path)) {
                message("Obsolete path detected: %s", path);
                obsoleteGames.add(game);
            }
        }

        gameManager.deleteGames(obsoleteGames);
        message("Removed %d obsolete games.", obsoleteGames.size());
        setProgress(0, 1);
    }

    @Override
    public Task<Void> processPath(Library library, Path path) {
        final LibraryHierarchy libraryHierarchy = new LibraryHierarchy(library);
        return taskService.submit((RunnableThrows) () -> processPath(libraryHierarchy, path));
    }

    @Override
    public void deleteGame(Game game) {
        if (dialogService.confirmationDialog("Are you sure you want to delete '%s'?", game.getName())) {
            gameManager.deleteGame(game);
        }
    }

    @Override
    public void deleteExcludedPaths(Collection<ExcludedPath> excludedPaths) {
        if (excludedPaths.isEmpty()) {
            return;
        }

        final ObservableList<ExcludedPath> items = FXCollections.observableArrayList(excludedPaths);
        if (dialogService.confirmationListDialog(items, "Are you sure you want to delete these %d excluded paths?", excludedPaths.size())) {
            excludedPathManager.deleteExcludedPaths(excludedPaths);
        }
    }

    @Override
    public void showGameDetails(Game game) {
        // FIXME: Handle exception while editing
        final Opt<Game> editedGame = screenService.showGameDetails(game);
        if (editedGame.isPresent()) {
            // TODO: Update in db
            System.out.println(editedGame);
        }
    }

    private void refreshCurrentLibrary(LibraryHierarchy libraryHierarchy) throws Exception {
        final Library library = libraryHierarchy.getCurrentLibrary();
        message("Refreshing library: '%s'[%s]", library.getName(), library.getPlatform());

        final ImmutableList<Path> directories = FileUtils.listChildDirectories(library.getPath());
        final int total = directories.size();
        for (int i = 0; i < total; i++) {
            checkStopped();

            setProgress(i, total);
            final Path path = directories.get(i);
            processPath(libraryHierarchy, path);
        }

        message("Finished refreshing library: '%s'[%s]\n", library.getName(), library.getPlatform());
        setProgress(0, 1);
    }

    private boolean processPath(LibraryHierarchy libraryHierarchy, Path path) throws Exception {
        if (gameManager.isGame(path)) {
            log.info("{} is already mapped, skipping...", path);
            return false;
        }

        if (libraryManager.isLibrary(path)) {
            log.info("{} is a library, skipping...", path);
            return false;
        }

        if (excludedPathManager.isExcluded(path)) {
            log.info("{} is excluded, skipping...", path);
            return false;
        }

        if (!isAutoSkip() && tryCreateLibrary(path, libraryHierarchy)) {
            return true;
        }

        message("Processing: %s...", path);
        final String name = getName(path);
        try {
            addPath(libraryHierarchy, path, name);
        } catch (SkipException e) {
            message("Skipping...");
        } catch (ExcludeException e) {
            message("Excluding...");
            excludedPathManager.addExcludedPath(path);
        }
        message("Finished processing %s.\n", path);
        return true;
    }

    private boolean tryCreateLibrary(Path path, LibraryHierarchy libraryHierarchy) throws Exception {
        checkStopped();

        if (!FileUtils.hasChildDirectories(path) || FileUtils.hasChildFiles(path)) {
            // Only directories that have sub-directories and no files can be libraries.
            return false;
        }

        final ImmutableList<Path> children = FileUtils.listChildDirectories(path);
        final Opt<LibraryDef> libraryDefOpt = dialogService.createLibraryDialog(path, children, libraryHierarchy.getPlatform());
        if (libraryDefOpt.isEmpty()) {
            return false;
        }

        final LibraryDef libraryDef = libraryDefOpt.get();
        final Library library = createLibraryFromDef(libraryDef);
        libraryHierarchy.pushLibrary(library);
        refreshCurrentLibrary(libraryHierarchy);
        libraryHierarchy.popLibrary();
        return true;
    }

    private Library createLibraryFromDef(LibraryDef libraryDef) {
        final Library library = libraryManager.createLibrary(libraryDef.getPath(), libraryDef.getPlatform(), libraryDef.getName());
        message("New library created: '%s'", library.getName());
        return library;
    }

    private String getName(Path path) {
        // Remove all metaData enclosed with '[]' from the game name.
        final String rawName = path.getFileName().toString();
        final String nameWithoutMetadata = META_DATA_PATTERN.matcher(rawName).replaceAll("");
        return SPACE_PATTERN.matcher(nameWithoutMetadata).replaceAll(" ");
    }

    private void addPath(LibraryHierarchy libraryHierarchy, Path path, String name) throws Exception {
        if (name.isEmpty()) {
            message("Empty name provided.");
            throw new SkipException();
        }

        final GamePlatform platform = libraryHierarchy.getPlatform();

        final SearchContext searchContext = new SearchContext(path, platform);
        final Opt<GameInfo> metacriticGameOpt = fetchGameInfo(metacriticManager, name, searchContext);
        if (metacriticGameOpt.isPresent()) {
            final GameInfo metacriticGame = metacriticGameOpt.get();
            log.debug("Metacritic gameInfo: {}", metacriticGame);

            final String metacriticName = metacriticGame.getName();
            final Opt<GameInfo> giantBombGameOpt = fetchGameInfo(giantBombManager, metacriticName, searchContext);
            if (!giantBombGameOpt.isPresent()) {
                message("Game not found on GiantBomb.");
            }

            final UnifiedGameInfo gameInfo = UnifiedGameInfo.from(metacriticGame, giantBombGameOpt);
            final Game game = gameManager.addGame(gameInfo, path, platform);
            libraryManager.addGameToLibraryHierarchy(game, libraryHierarchy);
        }
    }

    private Opt<GameInfo> fetchGameInfo(GameInfoProviderManager manager, String name, SearchContext context) throws Exception {
        checkStopped();

        messageProperty.bind(manager.messageProperty());
        fetchingProperty.bind(manager.fetchingProperty());
        try {
            return manager.fetchGameInfo(name, context);
        } finally {
            messageProperty.unbind();
            fetchingProperty.unbind();
        }
    }

    private boolean isAutoSkip() {
        return configService.<Boolean>property(ConfigType.AUTO_SKIP).get();
    }

    private void message(String format, Object... args) {
        message(String.format(format, args));
    }

    private void message(String message) {
        messageProperty.set(message);
    }

    private void setProgress(int current, int total) {
        progressProperty.setValue((double) current / total);
    }

    private void checkStopped() {
        if (Thread.interrupted()) {
            message("Stopping...");
            throw new GameDexException("Stopped.");
        }
    }

    private ObjectProperty<Opt<Path>> prevDirectoryProperty() {
        return configService.property(ConfigType.PREV_DIRECTORY);
    }
}
