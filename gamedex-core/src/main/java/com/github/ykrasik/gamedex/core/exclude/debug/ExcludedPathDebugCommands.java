package com.github.ykrasik.gamedex.core.exclude.debug;

import com.github.ykrasik.gamedex.common.debug.DebugCommands;
import com.github.ykrasik.gamedex.core.exclude.ExcludedPathManager;
import com.github.ykrasik.jerminal.api.annotation.Command;
import com.github.ykrasik.jerminal.api.annotation.ShellPath;
import com.github.ykrasik.jerminal.api.command.OutputPrinter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * @author Yevgeny Krasik
 */
@RequiredArgsConstructor
@ShellPath("excluded")
public class ExcludedPathDebugCommands implements DebugCommands {
    @NonNull private final ExcludedPathManager manager;

    @Command
    public void excludedPaths(OutputPrinter outputPrinter) {
        manager.getAllExcludedPaths().forEach(path -> outputPrinter.println(path.toString()));
    }
}
