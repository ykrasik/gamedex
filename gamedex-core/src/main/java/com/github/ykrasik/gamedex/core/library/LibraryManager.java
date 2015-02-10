package com.github.ykrasik.gamedex.core.library;

import com.github.ykrasik.gamedex.datamodel.GamePlatform;
import com.github.ykrasik.gamedex.datamodel.persistence.Game;
import com.github.ykrasik.gamedex.datamodel.persistence.Id;
import com.github.ykrasik.gamedex.datamodel.persistence.Library;
import javafx.beans.property.ReadOnlyListProperty;

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

    ReadOnlyListProperty<Library> librariesProperty();
}
