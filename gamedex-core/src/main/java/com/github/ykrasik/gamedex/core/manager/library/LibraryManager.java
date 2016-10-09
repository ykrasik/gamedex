package com.github.ykrasik.gamedex.core.manager.library;

import com.github.ykrasik.gamedex.core.ui.library.LibraryDef;
import com.github.ykrasik.gamedex.datamodel.Game;
import com.github.ykrasik.gamedex.datamodel.Library;
import com.github.ykrasik.gamedex.datamodel.flow.LibraryHierarchy;
import com.github.ykrasik.gamedex.datamodel.persistence.Id;
import javafx.beans.property.ReadOnlyListProperty;
import javafx.collections.ObservableList;

import java.nio.file.Path;

/**
 * @author Yevgeny Krasik
 */
public interface LibraryManager {
    Library createLibrary(LibraryDef libraryDef);
    ObservableList<Game> deleteLibrary(Library library);

    ObservableList<Library> getAllLibraries();
    Library getLibraryById(Id<Library> id);
    boolean isLibrary(Path path);

    void addGameToLibraryHierarchy(Game game, LibraryHierarchy libraryHierarchy);

    ReadOnlyListProperty<Library> librariesProperty();
}
