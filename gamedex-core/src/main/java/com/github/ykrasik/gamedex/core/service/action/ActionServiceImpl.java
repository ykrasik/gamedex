package com.github.ykrasik.gamedex.core.service.action;

import com.github.ykrasik.gamedex.common.util.FileUtils;
import com.github.ykrasik.gamedex.core.javafx.property.ThreadAwareBooleanProperty;
import com.github.ykrasik.gamedex.core.javafx.property.ThreadAwareDoubleProperty;
import com.github.ykrasik.gamedex.core.manager.exclude.ExcludedPathManager;
import com.github.ykrasik.gamedex.core.manager.game.GameManager;
import com.github.ykrasik.gamedex.core.manager.library.LibraryManager;
import com.github.ykrasik.gamedex.core.manager.path.PathManager;
import com.github.ykrasik.gamedex.core.manager.path.ProcessPathReturnValue;
import com.github.ykrasik.gamedex.core.manager.path.ProcessPathReturnValue.Type;
import com.github.ykrasik.gamedex.core.service.config.ConfigService;
import com.github.ykrasik.gamedex.core.service.dialog.DialogService;
import com.github.ykrasik.gamedex.core.service.task.TaskService;
import com.github.ykrasik.gamedex.core.ui.library.LibraryDef;
import com.github.ykrasik.gamedex.datamodel.flow.LibraryHierarchy;
import com.github.ykrasik.gamedex.datamodel.persistence.ExcludedPath;
import com.github.ykrasik.gamedex.datamodel.persistence.Game;
import com.github.ykrasik.gamedex.datamodel.persistence.Library;
import com.github.ykrasik.gamedex.datamodel.persistence.PathEntity;
import com.github.ykrasik.yava.option.Opt;
import com.github.ykrasik.yava.util.RunnableX;
import com.gs.collections.api.list.ImmutableList;
import javafx.beans.property.*;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Yevgeny Krasik
 */
@Slf4j
@Accessors(fluent = true)
@RequiredArgsConstructor
public class ActionServiceImpl implements ActionService {
    @Getter private final StringProperty messageProperty = new SimpleStringProperty();
    private final DoubleProperty progressProperty = new ThreadAwareDoubleProperty();
    private final BooleanProperty fetchingProperty = new ThreadAwareBooleanProperty();

    @NonNull private final ConfigService configService;
    @NonNull private final TaskService taskService;
    @NonNull private final DialogService dialogService;
    @NonNull private final GameManager gameManager;
    @NonNull private final LibraryManager libraryManager;
    @NonNull private final ExcludedPathManager excludedPathManager;
    @NonNull private final PathManager pathManager;

    @Override
    public ReadOnlyDoubleProperty progressProperty() {
        return progressProperty;
    }

    @Override
    public ReadOnlyBooleanProperty fetchingProperty() {
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
        doWithExceptionHandling(() -> {
            final Opt<LibraryDef> libraryDef = dialogService.addLibraryDialog();
            libraryDef.ifDefined(libraryManager::createLibrary);
        });
    }

    // TODO: Do this on the background thread and return a task?
    @Override
    public void addNewExcludedPath() {
        doWithExceptionHandling(() -> {
            final Opt<Path> excludedPath = dialogService.addExcludedPathDialog();
            excludedPath.ifDefined(excludedPathManager::addExcludedPath);
        });
    }

    @Override
    public Task<Void> refreshLibraries() {
        return taskService.submit(this::doRefreshLibraries);
    }

    private void doRefreshLibraries() throws Exception {
        message("Refreshing libraries...");

        final List<Library> libraries = libraryManager.getAllLibraries();
        for (Library library : libraries) {
            assertNotStopped();

            final LibraryHierarchy libraryHierarchy = new LibraryHierarchy(library);
            refreshCurrentLibrary(libraryHierarchy);
        }

        message("Finished refreshing libraries.");
    }

    @Override
    public Task<Void> cleanup() {
        return taskService.submit(this::doCleanup);
    }

    // TODO: Make this a total cleanup? libraries, excluded, evertyhing?
    private void doCleanup() {
        cleanupGames();
        cleanupLibraries();
        cleanupExcludedPaths();
        setProgress(0, 1);
    }

    private void cleanupGames() {
        message("Cleaning up games...");
        final List<Game> games = gameManager.getAllGames();
        final List<Game> obsoleteGames = getObsoleteEntries(games);
        gameManager.deleteGames(obsoleteGames);
        message("Removed %d obsolete games.", obsoleteGames.size());
    }

    private void cleanupLibraries() {
        message("Cleaning up libraries...");
        final List<Library> libraries = libraryManager.getAllLibraries();
        final List<Library> obsoleteLibraries = getObsoleteEntries(libraries);
        obsoleteLibraries.forEach(this::deleteLibrary);
        message("Removed %d obsolete libraries.", obsoleteLibraries.size());
    }

    private void cleanupExcludedPaths() {
        message("Cleaning up excluded paths...");
        final List<ExcludedPath> excludedPaths = excludedPathManager.getAllExcludedPaths();
        final List<ExcludedPath> obsoleteExcludedPaths = getObsoleteEntries(excludedPaths);
        excludedPathManager.deleteExcludedPaths(obsoleteExcludedPaths);
        message("Removed %d obsolete excluded paths.", obsoleteExcludedPaths.size());
    }

    private <T extends PathEntity> List<T> getObsoleteEntries(List<T> entries) {
        final List<T> obsoleteEntries = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) {
            assertNotStopped();

            setProgress(i, entries.size());
            final T entry = entries.get(i);
            final Path path = entry.getPath();
            if (!Files.exists(path)) {
                message("Obsolete path detected: %s", path);
                obsoleteEntries.add(entry);
            }
        }
        return obsoleteEntries;
    }

    @Override
    public Task<Void> processPath(Library library, Path path) {
        final LibraryHierarchy libraryHierarchy = new LibraryHierarchy(library);
        return taskService.submit(() -> processPath(libraryHierarchy, path));
    }

    @Override
    public void deleteGame(Game game) {
        // TODO: Confirmation dialog doesn't belong here.
        if (dialogService.confirmationDialog("Are you sure you want to delete the game '%s'?", game.getName())) {
            message("Deleting game '%s'...", game.getName());
            gameManager.deleteGame(game);
            message("Done.\n");
        }
    }

    @Override
    public void deleteExcludedPaths(ObservableList<ExcludedPath> excludedPaths) {
        if (excludedPaths.isEmpty()) {
            return;
        }

        // TODO: Confirmation dialog doesn't belong here.
        if (dialogService.confirmationListDialog(excludedPaths, Object::toString, "Are you sure you want to delete these %d excluded paths?", excludedPaths.size())) {
            excludedPathManager.deleteExcludedPaths(excludedPaths);
        }
    }

    @Override
    public void deleteLibraries(ObservableList<Library> libraries) {
        if (libraries.isEmpty()) {
            return;
        }

        // TODO: Confirmation dialog doesn't belong here.
        if (dialogService.confirmationListDialog(libraries, Library::getName, "Are you sure you want to delete these %d libraries?", libraries.size())) {
            libraries.forEach(this::deleteLibrary);
        }
    }

    private void deleteLibrary(Library library) {
        message("Deleting library '%s'...", library.getName());
        final ObservableList<Game> games = libraryManager.deleteLibrary(library);
        message("Done.");

        message("Deleting '%d' games linked to library '%s'...", games.size(), library.getName());
        gameManager.deleteGames(games);
        message("Done.\n");
    }

    private void refreshCurrentLibrary(LibraryHierarchy libraryHierarchy) throws Exception {
        final Library library = libraryHierarchy.getCurrentLibrary();
        message("Refreshing library: '%s'[%s]", library.getName(), library.getPlatform());

        final ImmutableList<Path> directories = FileUtils.listChildDirectories(library.getPath());
        final int total = directories.size();
        for (int i = 0; i < total; i++) {
            assertNotStopped();

            setProgress(i, total);
            final Path path = directories.get(i);
            processPath(libraryHierarchy, path);
        }

        message("Finished refreshing library: '%s'[%s]\n", library.getName(), library.getPlatform());
        setProgress(0, 1);
    }

    private void processPath(LibraryHierarchy libraryHierarchy, Path path) throws Exception {
        messageProperty.bind(pathManager.messageProperty());
        fetchingProperty.bind(pathManager.fetchingProperty());
        final ProcessPathReturnValue returnValue;
        try {
            returnValue = pathManager.processPath(libraryHierarchy, path);
        } finally {
            messageProperty.unbind();
            fetchingProperty.unbind();
        }

        if (returnValue.getType() == Type.NEW_LIBRARY) {
            final Library library = returnValue.getCreatedLibrary().get();
            libraryHierarchy.pushLibrary(library);
            refreshCurrentLibrary(libraryHierarchy);
            libraryHierarchy.popLibrary();
        }
    }

    private void setProgress(int current, int total) {
        progressProperty.setValue((double) current / total);
    }

    private void doWithExceptionHandling(RunnableX runnable) {
        try {
            runnable.runX();
        } catch (Exception e) {
            dialogService.showException(e);
        }
    }
}
