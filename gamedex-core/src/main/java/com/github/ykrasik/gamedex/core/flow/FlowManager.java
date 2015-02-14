package com.github.ykrasik.gamedex.core.flow;

import com.github.ykrasik.gamedex.datamodel.persistence.Library;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.concurrent.Task;

import java.nio.file.Path;

/**
 * @author Yevgeny Krasik
 */
// TODO: Everything should be done in this class, the controller should simply delegate here.
// TODO: This class should sub-divide responsibilities among the rest of the classes.
// TODO: Rename this to ActionManager? Each flow returns a Result class with properties?
public interface FlowManager {
    ReadOnlyStringProperty messageProperty();
    ReadOnlyDoubleProperty progressProperty();
    ReadOnlyDoubleProperty fetchProgressProperty();

    void stopTask(Task<Void> task);

    Task<Void> refreshLibraries();

    Task<Void> cleanupGames();

    Task<Void> processPath(Library library, Path path);
}
