package com.github.ykrasik.gamedex.core.service.action;

import com.github.ykrasik.gamedex.common.exception.RunnableThrows;
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
import com.github.ykrasik.opt.Opt;
import com.gs.collections.api.list.ImmutableList;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
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
import java.util.Collection;
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
            final Opt<LibraryDef> libraryDefOpt = dialogService.addLibraryDialog(configService.getPrevDirectory());
            if (libraryDefOpt.isPresent()) {
                final LibraryDef libraryDef = libraryDefOpt.get();
                configService.prevDirectoryProperty().set(Opt.of(libraryDef.getPath()));
                libraryManager.createLibrary(libraryDef);
            }
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
    public Task<Void> cleanupGames() {
        return taskService.submit(this::doCleanupGames);
    }

    // TODO: Make this a total cleanup? libraries, excluded, evertyhing?
    private void doCleanupGames() {
        message("Cleaning up games...");

        final List<Game> obsoleteGames = new ArrayList<>();
        final List<Game> games = gameManager.getAllGames();
        for (int i = 0; i < games.size(); i++) {
            assertNotStopped();

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
        // TODO: Confirmation dialog doesn't belong here.
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
        // TODO: Confirmation dialog doesn't belong here.
        if (dialogService.confirmationListDialog(items, "Are you sure you want to delete these %d excluded paths?", excludedPaths.size())) {
            excludedPathManager.deleteExcludedPaths(excludedPaths);
        }
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
        try {
            final ProcessPathReturnValue returnValue = pathManager.processPath(libraryHierarchy, path);
            if (returnValue.getType() == Type.NEW_LIBRARY) {
                final Library library = returnValue.getCreatedLibrary().get();
                libraryHierarchy.pushLibrary(library);
                refreshCurrentLibrary(libraryHierarchy);
                libraryHierarchy.popLibrary();
            }
        } finally {
            messageProperty.unbind();
            fetchingProperty.unbind();
        }
    }

    private void setProgress(int current, int total) {
        progressProperty.setValue((double) current / total);
    }

    private void doWithExceptionHandling(RunnableThrows runnable) {
        try {
            runnable.run();
        } catch (Exception e) {
            dialogService.showException(e);
        }
    }
}
