package com.github.ykrasik.gamedex.core.service.action.debug;

import com.github.ykrasik.gamedex.common.debug.DebugCommands;
import com.github.ykrasik.gamedex.core.manager.library.LibraryManager;
import com.github.ykrasik.gamedex.core.service.action.ActionService;
import com.github.ykrasik.gamedex.datamodel.persistence.Id;
import com.github.ykrasik.gamedex.datamodel.persistence.Library;
import com.github.ykrasik.jaci.api.*;
import javafx.concurrent.Task;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.nio.file.Paths;

/**
 * @author Yevgeny Krasik
 */
@RequiredArgsConstructor
@CommandPath("action")
public class ActionServiceDebugCommands implements DebugCommands {
    @NonNull private final ActionService actionService;
    @NonNull private final LibraryManager libraryManager;

    private CommandOutput output;

    @Command
    public void refreshLibraries() throws Exception {
        final Task<Void> task = actionService.refreshLibraries();
        printFailureToOutput(task);
        task.setOnSucceeded(e -> output.message("Finished refreshing libraries!"));
    }

    @Command
    public void processPath(@IntParam("libraryId") int id,
                            @StringParam("path") String path) throws Exception {
        final Library library = libraryManager.getLibraryById(new Id<>(id));
        final Task<Void> task = actionService.processPath(library, Paths.get(path));
        printFailureToOutput(task);
        task.setOnSucceeded(e -> output.message("Finished processing path: %s", path));
    }

    private void printFailureToOutput(Task<Void> task) {
        task.setOnFailed(event -> output.message("Error : %s", task.getException().getMessage()));
    }
}
