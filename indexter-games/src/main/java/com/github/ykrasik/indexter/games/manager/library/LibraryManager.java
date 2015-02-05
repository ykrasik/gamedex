package com.github.ykrasik.indexter.games.manager.library;

import com.github.ykrasik.indexter.games.datamodel.GamePlatform;
import com.github.ykrasik.indexter.games.datamodel.persistence.Game;
import com.github.ykrasik.indexter.games.datamodel.persistence.Library;
import com.github.ykrasik.indexter.id.Id;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.collections.ObservableList;

import java.nio.file.Path;
import java.util.Optional;

/**
 * @author Yevgeny Krasik
 */
public interface LibraryManager {
    Library createLibrary(String name, Path path, GamePlatform platform);
    void deleteLibrary(Library library);

    Library getLibraryById(Id<Library> id);
    Optional<Library> getLibraryByPath(Path path);
    boolean isLibrary(Path path);
    ObservableList<Library> getAllLibraries();

    void addGameToLibrary(Game game, Library library);

    ReadOnlyObjectProperty<ObservableList<Library>> itemsProperty();

    // FIXME: This should be in it's own manager.
    void setExcluded(Path path);
    boolean isExcluded(Path path);
}
