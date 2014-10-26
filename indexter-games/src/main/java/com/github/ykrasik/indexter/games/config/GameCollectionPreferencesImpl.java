package com.github.ykrasik.indexter.games.config;

import com.github.ykrasik.indexter.config.preferences.PreferencesManager;
import com.github.ykrasik.indexter.games.datamodel.GamePlatform;
import com.google.common.annotations.VisibleForTesting;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * @author Yevgeny Krasik
 */
public class GameCollectionPreferencesImpl implements GameCollectionPreferences {
    private static final String PREV_DIRECTORY = "prevDirectory";
    private static final String LIBRARIES = "libraries";

    private final PreferencesManager manager;

    public GameCollectionPreferencesImpl() {
        this.manager = new PreferencesManager(GameCollectionPreferences.class);
    }

    @Override
    public Optional<File> getPrevDirectory() {
        return manager.getFile(PREV_DIRECTORY);
    }

    @Override
    public void setPrevDirectory(File prevDirectory) {
        manager.setFile(PREV_DIRECTORY, prevDirectory);
    }

    @Override
    public Map<Path, GamePlatform> getLibraries() {
        final Map<Path, GamePlatform> libraries = manager.getMap(LIBRARIES, Paths::get, GamePlatform::valueOf);
        // If libraries is empty, Collections.emptyMap is returned, which is unmodifiable.
        return libraries.isEmpty() ? new HashMap<>(1) : libraries;
    }

    @Override
    public void setLibraries(Map<Path, GamePlatform> libraries) {
        manager.putMap(LIBRARIES, libraries, Path::toString, GamePlatform::name);
    }

    @VisibleForTesting
    public void clearLibraries() {
        manager.clear(LIBRARIES);
    }
}
