package com.github.ykrasik.indexter.games.library;

import com.github.ykrasik.indexter.games.datamodel.Library;

import java.nio.file.Path;
import java.util.List;

/**
 * @author Yevgeny Krasik
 */
public interface LibraryManager {
    List<Library> getLibraries();

    boolean isLibrary(Path path);

    boolean isExcluded(Path path);

    void addLibrary(Library library);

    void addSubLibraries(List<Library> subLibraries);
}
