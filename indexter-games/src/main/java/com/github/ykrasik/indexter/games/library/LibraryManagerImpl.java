package com.github.ykrasik.indexter.games.library;

import com.github.ykrasik.indexter.games.config.GameCollectionConfig;
import com.github.ykrasik.indexter.games.datamodel.Library;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author Yevgeny Krasik
 */
public class LibraryManagerImpl implements LibraryManager {
    private final GameCollectionConfig config;

    public LibraryManagerImpl(GameCollectionConfig config) {
        this.config = Objects.requireNonNull(config);
    }

    @Override
    public List<Library> getLibraries() {
        final Map<Path, Library> libraries = config.getLibraries();
        final Map<Path, Library> subLibraries = config.getSubLibraries();
        final List<Library> allLibraries = new ArrayList<>(libraries.size() + subLibraries.size());
        allLibraries.addAll(libraries.values());
        allLibraries.addAll(subLibraries.values());
        return allLibraries;
    }

    @Override
    public boolean isLibrary(Path path) {
        return config.getLibraries().containsKey(path) ||
            config.getSubLibraries().containsKey(path);
    }

    @Override
    public void addLibrary(Library library) {
        config.addLibrary(library);
    }

    @Override
    public void addSubLibrary(Library subLibrary) {
        config.addSubLibrary(subLibrary);
    }

    @Override
    public void setExcluded(Path path) {
        config.addExcludedPath(path);
    }

    @Override
    public boolean isExcluded(Path path) {
        return config.getExcludedPaths().contains(path);
    }
}
