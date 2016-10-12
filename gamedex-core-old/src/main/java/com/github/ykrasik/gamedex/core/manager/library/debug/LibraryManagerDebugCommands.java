package com.github.ykrasik.gamedex.core.manager.library.debug;

import com.github.ykrasik.gamedex.core.debug.DebugCommands;
import com.github.ykrasik.gamedex.core.manager.library.LibraryManager;
import com.github.ykrasik.gamedex.core.persistence.Id;
import com.github.ykrasik.gamedex.datamodel.Library;
import com.github.ykrasik.jaci.api.*;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Yevgeny Krasik
 */
@RequiredArgsConstructor
@CommandPath("library")
public class LibraryManagerDebugCommands implements DebugCommands {
    @NonNull private final LibraryManager manager;

    private CommandOutput output;
    
    @Command
    public void getById(@IntParam("id") int id) throws Exception {
        final Library library = manager.getLibraryById(new Id<>(id));
        output.message(library.toString());
    }

    @Command
    public void isLibrary(@StringParam("path") String path) throws Exception {
        output.message(String.valueOf(manager.isLibrary(Paths.get(path))));
    }

    @Command
    public void all() throws Exception {
        final List<Library> libraries = manager.getAllLibraries();
        libraries.forEach(library -> output.message(library.toString()));
    }

    @Command
    public void delete(@IntParam("id") int id) throws Exception {
        final Library library = manager.getLibraryById(new Id<>(id));
        manager.deleteLibrary(library);
    }

    @Command
    public void clear() throws Exception {
        final List<Library> libraries = new ArrayList<>(manager.getAllLibraries());
        libraries.forEach(manager::deleteLibrary);
    }
}
