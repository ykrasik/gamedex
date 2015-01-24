package com.github.ykrasik.indexter.games.manager.library;

import com.github.ykrasik.indexter.games.datamodel.Library;
import com.github.ykrasik.indexter.games.datamodel.LocalGame;
import com.github.ykrasik.indexter.games.datamodel.LocalLibrary;
import com.github.ykrasik.indexter.id.Id;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.collections.ObservableList;

import java.nio.file.Path;
import java.util.Optional;

/**
 * @author Yevgeny Krasik
 */
public interface LibraryManager {
    LocalLibrary addLibrary(Library library);
    void deleteLibrary(LocalLibrary library);

    LocalLibrary getLibraryById(Id<LocalLibrary> id);
    Optional<LocalLibrary> getLibraryByPath(Path path);
    boolean isLibrary(Path path);
    ObservableList<LocalLibrary> getAllLibraries();

    void addGameToLibrary(LocalGame game, LocalLibrary library);

    ReadOnlyObjectProperty<ObservableList<LocalLibrary>> itemsProperty();

    // FIXME: This should be in it's own manager.
    void setExcluded(Path path);
    boolean isExcluded(Path path);
}
