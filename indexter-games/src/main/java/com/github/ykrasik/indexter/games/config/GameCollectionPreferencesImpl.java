package com.github.ykrasik.indexter.games.config;

import com.github.ykrasik.indexter.config.preferences.PreferencesManager;

import java.io.File;
import java.util.Optional;

/**
 * @author Yevgeny Krasik
 */
public class GameCollectionPreferencesImpl implements GameCollectionPreferences {
    private static final String PREV_DIRECTORY = "prevDirectory";

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
}
