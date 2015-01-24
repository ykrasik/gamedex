package com.github.ykrasik.indexter.games.debug;

import com.github.ykrasik.indexter.debug.DebugCommands;
import com.github.ykrasik.indexter.games.datamodel.LocalLibrary;
import com.github.ykrasik.indexter.games.manager.library.LibraryManager;
import com.github.ykrasik.indexter.games.manager.scan.ScanManager;
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
@ShellPath("scan")
public class ScanManagerDebugCommands implements DebugCommands {
    private final ScanManager scanManager;
    private final LibraryManager libraryManager;

    public ScanManagerDebugCommands(ScanManager scanManager, LibraryManager libraryManager) {
        this.scanManager = Objects.requireNonNull(scanManager);
        this.libraryManager = Objects.requireNonNull(libraryManager);
    }

    @Command
    public void refreshLibraries(OutputPrinter outputPrinter) throws Exception {
        scanManager.refreshLibraries(t -> outputPrinter.println("Error refreshing libraries: %s", t.getMessage()));
        outputPrinter.println("Finished refreshing libraries!");
    }

    @Command
    public void processPath(OutputPrinter outputPrinter,
                            @IntParam("libraryId") int id,
                            @StringParam("path") String path) throws Exception {
        final LocalLibrary library = libraryManager.getLibraryById(new Id<>(id));
        scanManager.processPath(library, Paths.get(path), t -> outputPrinter.println("Error processing path: %s", t.getMessage()));
        outputPrinter.println("Finished processing path: %s", path);
    }
}
