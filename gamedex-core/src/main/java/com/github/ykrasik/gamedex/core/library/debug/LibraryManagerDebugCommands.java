package com.github.ykrasik.gamedex.core.library.debug;

import com.github.ykrasik.gamedex.common.debug.DebugCommands;
import com.github.ykrasik.gamedex.core.library.LibraryManager;
import com.github.ykrasik.gamedex.datamodel.persistence.Id;
import com.github.ykrasik.gamedex.datamodel.persistence.Library;
import com.github.ykrasik.jerminal.api.annotation.Command;
import com.github.ykrasik.jerminal.api.annotation.IntParam;
import com.github.ykrasik.jerminal.api.annotation.ShellPath;
import com.github.ykrasik.jerminal.api.annotation.StringParam;
import com.github.ykrasik.jerminal.api.command.OutputPrinter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Yevgeny Krasik
 */
@RequiredArgsConstructor
@ShellPath("library")
public class LibraryManagerDebugCommands implements DebugCommands {
    @NonNull private final LibraryManager manager;

    @Command
    public void getById(OutputPrinter outputPrinter, @IntParam("id") int id) throws Exception {
        final Library library = manager.getLibraryById(new Id<>(id));
        outputPrinter.println(library.toString());
    }

    @Command
    public void isLibrary(OutputPrinter outputPrinter, @StringParam("path") String path) throws Exception {
        outputPrinter.println(String.valueOf(manager.isLibrary(Paths.get(path))));
    }

    @Command
    public void all(OutputPrinter outputPrinter) throws Exception {
        final List<Library> libraries = manager.getAllLibraries();
        libraries.forEach(library -> outputPrinter.println(library.toString()));
    }

    @Command
    public void delete(OutputPrinter outputPrinter, @IntParam("id") int id) throws Exception {
        final Library library = manager.getLibraryById(new Id<>(id));
        manager.deleteLibrary(library);
    }

    @Command
    public void clear(OutputPrinter outputPrinter) throws Exception {
        final List<Library> libraries = new ArrayList<>(manager.getAllLibraries());
        libraries.forEach(manager::deleteLibrary);
    }
}
