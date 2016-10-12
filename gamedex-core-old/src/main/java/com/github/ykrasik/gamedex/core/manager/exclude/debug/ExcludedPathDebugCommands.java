package com.github.ykrasik.gamedex.core.manager.exclude.debug;

import com.github.ykrasik.gamedex.core.debug.DebugCommands;
import com.github.ykrasik.gamedex.core.manager.exclude.ExcludedPathManager;
import com.github.ykrasik.jaci.api.Command;
import com.github.ykrasik.jaci.api.CommandOutput;
import com.github.ykrasik.jaci.api.CommandPath;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * @author Yevgeny Krasik
 */
@RequiredArgsConstructor
@CommandPath("excluded")
public class ExcludedPathDebugCommands implements DebugCommands {
    @NonNull private final ExcludedPathManager manager;

    private CommandOutput output;

    @Command
    public void excludedPaths() {
        manager.getAllExcludedPaths().forEach(path -> output.message(path.toString()));
    }
}
