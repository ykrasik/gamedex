package com.github.ykrasik.indexter.games.debug;

import com.github.ykrasik.indexter.debug.DebugCommands;
import com.github.ykrasik.indexter.games.datamodel.persistence.Library;
import com.github.ykrasik.indexter.games.manager.library.LibraryManager;
import com.github.ykrasik.indexter.id.Id;
import com.github.ykrasik.jerminal.api.annotation.Command;
import com.github.ykrasik.jerminal.api.annotation.IntParam;
import com.github.ykrasik.jerminal.api.annotation.ShellPath;
import com.github.ykrasik.jerminal.api.annotation.StringParam;
import com.github.ykrasik.jerminal.api.command.OutputPrinter;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author Yevgeny Krasik
 */
@ShellPath("library")
public class LibraryManagerDebugCommands implements DebugCommands {
    private final LibraryManager libraryManager;

    public LibraryManagerDebugCommands(LibraryManager libraryManager) {
        this.libraryManager = Objects.requireNonNull(libraryManager);
    }

    @Command
    public void getById(OutputPrinter outputPrinter, @IntParam("id") int id) throws Exception {
        final Library library = libraryManager.getLibraryById(new Id<>(id));
        outputPrinter.println(library.toString());
    }

    @Command
    public void isLibrary(OutputPrinter outputPrinter, @StringParam("path") String path) throws Exception {
        outputPrinter.println(String.valueOf(libraryManager.isLibrary(Paths.get(path))));
    }

    @Command
    public void all(OutputPrinter outputPrinter) throws Exception {
        final List<Library> libraries = libraryManager.getAllLibraries();
        libraries.forEach(library -> outputPrinter.println(library.toString()));
    }

    @Command
    public void delete(OutputPrinter outputPrinter, @IntParam("id") int id) throws Exception {
        final Library library = libraryManager.getLibraryById(new Id<>(id));
        libraryManager.deleteLibrary(library);
    }

    @Command
    public void clear(OutputPrinter outputPrinter) throws Exception {
        final List<Library> libraries = new ArrayList<>(libraryManager.getAllLibraries());
        libraries.forEach(libraryManager::deleteLibrary);
    }
}
