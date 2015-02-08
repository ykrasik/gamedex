package com.github.ykrasik.indexter.games.manager.library;

import com.github.ykrasik.indexter.games.datamodel.GamePlatform;
import com.github.ykrasik.indexter.games.datamodel.persistence.Game;
import com.github.ykrasik.indexter.games.datamodel.persistence.Library;
import com.github.ykrasik.indexter.id.Id;
import javafx.beans.property.ReadOnlyProperty;
import javafx.collections.ObservableList;

import java.nio.file.Path;
import java.util.List;

/**
 * @author Yevgeny Krasik
 */
public interface LibraryManager {
    Library createLibrary(String name, Path path, GamePlatform platform);
    void deleteLibrary(Library library);

    List<Library> getAllLibraries();
    Library getLibraryById(Id<Library> id);
    boolean isLibrary(Path path);

    void addGameToLibrary(Game game, Library library);

    ReadOnlyProperty<ObservableList<Library>> librariesProperty();
}
