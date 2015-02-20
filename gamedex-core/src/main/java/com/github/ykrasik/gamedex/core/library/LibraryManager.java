package com.github.ykrasik.gamedex.core.library;

import com.github.ykrasik.gamedex.datamodel.GamePlatform;
import com.github.ykrasik.gamedex.datamodel.flow.LibraryHierarchy;
import com.github.ykrasik.gamedex.datamodel.persistence.Game;
import com.github.ykrasik.gamedex.datamodel.persistence.Id;
import com.github.ykrasik.gamedex.datamodel.persistence.Library;
import javafx.beans.property.ReadOnlyListProperty;
import javafx.collections.ObservableList;

import java.nio.file.Path;

/**
 * @author Yevgeny Krasik
 */
public interface LibraryManager {
    Library createLibrary(Path path, GamePlatform platform, String name);
    void deleteLibrary(Library library);

    ObservableList<Library> getAllLibraries();
    Library getLibraryById(Id<Library> id);
    boolean isLibrary(Path path);

    void addGameToLibraryHierarchy(Game game, LibraryHierarchy libraryHierarchy);

    ReadOnlyListProperty<Library> librariesProperty();
}
