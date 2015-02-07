package com.github.ykrasik.indexter.games.debug;

import com.github.ykrasik.indexter.debug.DebugCommands;
import com.github.ykrasik.indexter.games.config.GameCollectionConfig;
import com.github.ykrasik.jerminal.api.annotation.Command;
import com.github.ykrasik.jerminal.api.annotation.ShellPath;
import com.github.ykrasik.jerminal.api.command.OutputPrinter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.Objects;

/**
 * @author Yevgeny Krasik
 */
@RequiredArgsConstructor
@ShellPath("config")
public class ConfigDebugCommands implements DebugCommands {
    @NonNull private final GameCollectionConfig config;

    @Command
    public void excludedPaths(OutputPrinter outputPrinter) {
        config.getExcludedPaths().forEach(path -> outputPrinter.println(path.toString()));
    }
}
