package com.github.ykrasik.indexter.games.manager.library;

import com.github.ykrasik.indexter.AbstractService;
import com.github.ykrasik.indexter.games.config.GameCollectionConfig;
import com.github.ykrasik.indexter.games.datamodel.Library;
import com.github.ykrasik.indexter.games.datamodel.LocalGame;
import com.github.ykrasik.indexter.games.datamodel.LocalLibrary;
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

    private ObservableList<LocalLibrary> libraries = FXCollections.emptyObservableList();
    private ObjectProperty<ObservableList<LocalLibrary>> itemsProperty = new SimpleObjectProperty<>();

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
    public LocalLibrary addLibrary(Library library) {
        final LocalLibrary localLibrary = persistenceService.addLibrary(library);

        // Update cache.
        PlatformUtils.runLater(() -> libraries.add(localLibrary));

        return localLibrary;
    }

    @Override
    public void deleteLibrary(LocalLibrary library) {
        // Delete from db.
        persistenceService.deleteLibrary(library.getId());

        // Delete from cache.
        PlatformUtils.runLater(() -> libraries.remove(library));
    }

    @Override
    public LocalLibrary getLibraryById(Id<LocalLibrary> id) {
        return persistenceService.getLibraryById(id);
    }

    @Override
    public Optional<LocalLibrary> getLibraryByPath(Path path) {
        return persistenceService.getLibraryByPath(path);
    }

    @Override
    public boolean isLibrary(Path path) {
        return getLibraryByPath(path).isPresent();
    }

    @Override
    public ObservableList<LocalLibrary> getAllLibraries() {
        return libraries;
    }

    @Override
    public void addGameToLibrary(LocalGame game, LocalLibrary library) {
        persistenceService.addGameToLibrary(game, library);
    }

    @Override
    public ReadOnlyObjectProperty<ObservableList<LocalLibrary>> itemsProperty() {
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
