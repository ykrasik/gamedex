package com.github.ykrasik.indexter.games.manager.library;

import com.github.ykrasik.indexter.AbstractService;
import com.github.ykrasik.indexter.games.datamodel.GamePlatform;
import com.github.ykrasik.indexter.games.datamodel.persistence.Game;
import com.github.ykrasik.indexter.games.datamodel.persistence.Library;
import com.github.ykrasik.indexter.games.persistence.PersistenceService;
import com.github.ykrasik.indexter.id.Id;
import com.github.ykrasik.indexter.util.PlatformUtils;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.util.List;

/**
 * @author Yevgeny Krasik
 */
@Slf4j
@RequiredArgsConstructor
public class LibraryManagerImpl extends AbstractService implements LibraryManager {
    @NonNull private final PersistenceService persistenceService;

    private final ObjectProperty<ObservableList<Library>> librariesProperty = new SimpleObjectProperty<>();
    private ObservableList<Library> libraries = FXCollections.emptyObservableList();

    @Override
    protected void doStart() throws Exception {
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
        log.info("Added library: {}", library);

        // Update cache.
        PlatformUtils.runLaterIfNecessary(() -> libraries.add(library));
        return library;
    }

    @Override
    public void deleteLibrary(Library library) {
        persistenceService.deleteLibrary(library.getId());
        log.info("Deleted library: {}", library);

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
    public ReadOnlyProperty<ObservableList<Library>> librariesProperty() {
        return librariesProperty;
    }
}
