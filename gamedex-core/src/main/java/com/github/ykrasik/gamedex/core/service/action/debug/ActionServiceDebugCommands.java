package com.github.ykrasik.gamedex.core.service.action.debug;

import com.github.ykrasik.gamedex.common.debug.DebugCommands;
import com.github.ykrasik.gamedex.core.service.action.ActionService;
import com.github.ykrasik.gamedex.core.manager.library.LibraryManager;
import com.github.ykrasik.gamedex.datamodel.persistence.Id;
import com.github.ykrasik.gamedex.datamodel.persistence.Library;
import com.github.ykrasik.jerminal.api.annotation.Command;
import com.github.ykrasik.jerminal.api.annotation.IntParam;
import com.github.ykrasik.jerminal.api.annotation.ShellPath;
import com.github.ykrasik.jerminal.api.annotation.StringParam;
import com.github.ykrasik.jerminal.api.command.OutputPrinter;
import javafx.concurrent.Task;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.nio.file.Paths;

/**
 * @author Yevgeny Krasik
 */
@RequiredArgsConstructor
@ShellPath("action")
public class ActionServiceDebugCommands implements DebugCommands {
    @NonNull private final ActionService actionService;
    @NonNull private final LibraryManager libraryManager;

    @Command
    public void refreshLibraries(OutputPrinter outputPrinter) throws Exception {
        final Task<Void> task = actionService.refreshLibraries();
        printFailureToOutput(task, outputPrinter);
        task.setOnSucceeded(e -> outputPrinter.println("Finished refreshing libraries!"));
    }

    @Command
    public void processPath(OutputPrinter outputPrinter,
                            @IntParam("libraryId") int id,
                            @StringParam("path") String path) throws Exception {
        final Library library = libraryManager.getLibraryById(new Id<>(id));
        final Task<Void> task = actionService.processPath(library, Paths.get(path));
        printFailureToOutput(task, outputPrinter);
        task.setOnSucceeded(e -> outputPrinter.println("Finished processing path: %s", path));
    }

    private void printFailureToOutput(Task<Void> task, OutputPrinter outputPrinter) {
        task.setOnFailed(event -> outputPrinter.println("Error : %s", task.getException().getMessage()));
    }
}
