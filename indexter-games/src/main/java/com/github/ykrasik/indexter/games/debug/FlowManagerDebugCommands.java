package com.github.ykrasik.indexter.games.debug;

import com.github.ykrasik.indexter.debug.DebugCommands;
import com.github.ykrasik.indexter.games.datamodel.persistence.Library;
import com.github.ykrasik.indexter.games.manager.library.LibraryManager;
import com.github.ykrasik.indexter.games.manager.flow.FlowManager;
import com.github.ykrasik.indexter.id.Id;
import com.github.ykrasik.jerminal.api.annotation.Command;
import com.github.ykrasik.jerminal.api.annotation.IntParam;
import com.github.ykrasik.jerminal.api.annotation.ShellPath;
import com.github.ykrasik.jerminal.api.annotation.StringParam;
import com.github.ykrasik.jerminal.api.command.OutputPrinter;

import java.nio.file.Paths;
import java.util.Objects;

/**
 * @author Yevgeny Krasik
 */
@ShellPath("flow")
public class FlowManagerDebugCommands implements DebugCommands {
    private final FlowManager flowManager;
    private final LibraryManager libraryManager;

    public FlowManagerDebugCommands(FlowManager flowManager, LibraryManager libraryManager) {
        this.flowManager = Objects.requireNonNull(flowManager);
        this.libraryManager = Objects.requireNonNull(libraryManager);
    }

    @Command
    public void refreshLibraries(OutputPrinter outputPrinter) throws Exception {
        flowManager.refreshLibraries(t -> outputPrinter.println("Error refreshing libraries: %s", t.getMessage()));
        outputPrinter.println("Finished refreshing libraries!");
    }

    @Command
    public void processPath(OutputPrinter outputPrinter,
                            @IntParam("libraryId") int id,
                            @StringParam("path") String path) throws Exception {
        final Library library = libraryManager.getLibraryById(new Id<>(id));
        flowManager.processPath(library, Paths.get(path), t -> outputPrinter.println("Error processing path: %s", t.getMessage()));
        outputPrinter.println("Finished processing path: %s", path);
    }
}
