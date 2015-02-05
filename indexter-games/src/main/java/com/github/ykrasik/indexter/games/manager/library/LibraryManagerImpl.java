package com.github.ykrasik.indexter.games.manager.library;

import com.github.ykrasik.indexter.AbstractService;
import com.github.ykrasik.indexter.games.config.GameCollectionConfig;
import com.github.ykrasik.indexter.games.datamodel.GamePlatform;
import com.github.ykrasik.indexter.games.datamodel.persistence.Game;
import com.github.ykrasik.indexter.games.datamodel.persistence.Library;
import com.github.ykrasik.indexter.games.persistence.PersistenceService;
import com.github.ykrasik.indexter.id.Id;
import com.github.ykrasik.indexter.util.PlatformUtils;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

/**
 * @author Yevgeny Krasik
 */
public class LibraryManagerImpl extends AbstractService implements LibraryManager {
    private final GameCollectionConfig config;
    private final PersistenceService persistenceService;

    private ObservableList<Library> libraries = FXCollections.emptyObservableList();
    private ObjectProperty<ObservableList<Library>> itemsProperty = new SimpleObjectProperty<>();

    public LibraryManagerImpl(PersistenceService persistenceService, GameCollectionConfig config) {
        this.persistenceService = Objects.requireNonNull(persistenceService);
        this.config = Objects.requireNonNull(config);
    }

    @Override
    protected void doStart() throws Exception {
        this.libraries = FXCollections.observableArrayList(persistenceService.getAllLibraries());
        this.itemsProperty = new SimpleObjectProperty<>(libraries);
    }

    @Override
    protected void doStop() throws Exception {

    }

    @Override
    public Library createLibrary(String name, Path path, GamePlatform platform) {
        final Library library = persistenceService.addLibrary(name, path, platform);

        // Update cache.
        PlatformUtils.runLater(() -> libraries.add(library));

        return library;
    }

    @Override
    public void deleteLibrary(Library library) {
        // Delete from db.
        persistenceService.deleteLibrary(library.getId());

        // Delete from cache.
        PlatformUtils.runLater(() -> libraries.remove(library));
    }

    @Override
    public Library getLibraryById(Id<Library> id) {
        return persistenceService.getLibraryById(id);
    }

    @Override
    public Optional<Library> getLibraryByPath(Path path) {
        return persistenceService.getLibraryByPath(path);
    }

    @Override
    public boolean isLibrary(Path path) {
        return getLibraryByPath(path).isPresent();
    }

    @Override
    public ObservableList<Library> getAllLibraries() {
        return libraries;
    }

    @Override
    public void addGameToLibrary(Game game, Library library) {
        persistenceService.addGameToLibrary(game, library);
    }

    @Override
    public ReadOnlyObjectProperty<ObservableList<Library>> itemsProperty() {
        return itemsProperty;
    }

    @Override
    public void setExcluded(Path path) {
        config.addExcludedPath(path);
    }

    @Override
    public boolean isExcluded(Path path) {
        return config.getExcludedPaths().contains(path);
    }

    // This is a hack... it seems that the gridView that is bound to this doesn't know how to refresh itself properly.
    private void refreshItemsProperty() {
        itemsProperty.setValue(FXCollections.emptyObservableList());
        itemsProperty.setValue(libraries);
    }
}
