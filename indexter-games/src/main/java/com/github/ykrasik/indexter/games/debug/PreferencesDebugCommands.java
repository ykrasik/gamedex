package com.github.ykrasik.indexter.games.debug;

import com.github.ykrasik.indexter.debug.DebugCommands;
import com.github.ykrasik.indexter.games.config.GameCollectionConfigImpl;
import com.github.ykrasik.indexter.games.datamodel.Library;
import com.github.ykrasik.jerminal.api.annotation.Command;
import com.github.ykrasik.jerminal.api.annotation.ShellPath;
import com.github.ykrasik.jerminal.api.command.OutputPrinter;
import com.google.common.annotations.VisibleForTesting;

import java.nio.file.Path;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

/**
 * @author Yevgeny Krasik
 */
@ShellPath("preferences")
public class PreferencesDebugCommands implements DebugCommands {
    private final GameCollectionConfigImpl preferences;

    public PreferencesDebugCommands(GameCollectionConfigImpl preferences) {
        this.preferences = Objects.requireNonNull(preferences);
    }

    @ShellPath("libraries")
    @Command("get")
    public void getLibraries(OutputPrinter outputPrinter) throws Exception {
        final Map<Path, Library> libraries = preferences.getLibraries();
        if (libraries.isEmpty()) {
            outputPrinter.println("Empty.");
        } else {
            for (Entry<Path, Library> entry : libraries.entrySet()) {
                outputPrinter.println("%s -> %s", entry.getKey(), entry.getValue());
            }
        }
    }

    @VisibleForTesting
    @ShellPath("libraries")
    @Command("clear")
    public void clearLibraries(OutputPrinter outputPrinter) throws Exception {
        preferences.clearLibraries();
    }
}
