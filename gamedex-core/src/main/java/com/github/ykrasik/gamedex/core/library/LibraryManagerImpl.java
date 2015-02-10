package com.github.ykrasik.gamedex.core.library;

import com.github.ykrasik.gamedex.common.service.AbstractService;
import com.github.ykrasik.gamedex.common.util.PlatformUtils;
import com.github.ykrasik.gamedex.datamodel.GamePlatform;
import com.github.ykrasik.gamedex.datamodel.persistence.Game;
import com.github.ykrasik.gamedex.datamodel.persistence.Id;
import com.github.ykrasik.gamedex.datamodel.persistence.Library;
import com.github.ykrasik.gamedex.persistence.PersistenceService;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ReadOnlyListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.nio.file.Path;
import java.util.List;

/**
 * @author Yevgeny Krasik
 */
@RequiredArgsConstructor
public class LibraryManagerImpl extends AbstractService implements LibraryManager {
    @NonNull private final PersistenceService persistenceService;

    private final ListProperty<Library> librariesProperty = new SimpleListProperty<>();
    private ObservableList<Library> libraries = FXCollections.emptyObservableList();

    @Override
    protected void doStart() throws Exception {
        LOG.info("Loading libraries...");
        libraries = FXCollections.observableArrayList(persistenceService.getAllLibraries());
        librariesProperty.setValue(libraries);
        LOG.info("Libraries: {}", libraries.size());
    }

    @Override
    protected void doStop() throws Exception {

    }

    @Override
    public Library createLibrary(String name, Path path, GamePlatform platform) {
        final Library library = persistenceService.addLibrary(name, path, platform);
        LOG.info("Added library: {}", library);

        // Update cache.
        PlatformUtils.runLaterIfNecessary(() -> libraries.add(library));
        return library;
    }

    @Override
    public void deleteLibrary(Library library) {
        persistenceService.deleteLibrary(library.getId());
        LOG.info("Deleted library: {}", library);

        // Delete from cache.
        PlatformUtils.runLaterIfNecessary(() -> libraries.remove(library));
    }

    @Override
    public List<Library> getAllLibraries() {
        return libraries;
    }

    @Override
    public Library getLibraryById(Id<Library> id) {
        return persistenceService.getLibraryById(id);
    }

    @Override
    public boolean isLibrary(Path path) {
        return persistenceService.hasLibraryForPath(path);
    }

    @Override
    public void addGameToLibrary(Game game, Library library) {
        persistenceService.addGameToLibrary(game, library);
    }

    @Override
    public ReadOnlyListProperty<Library> librariesProperty() {
        return librariesProperty;
    }
}
