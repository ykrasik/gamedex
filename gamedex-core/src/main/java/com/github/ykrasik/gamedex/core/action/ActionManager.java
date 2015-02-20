package com.github.ykrasik.gamedex.core.action;

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
// TODO: Need 2 threads - one to search and fetch games, one to fetch thumbnails & posters.
public interface ActionManager {
    ReadOnlyStringProperty messageProperty();
    ReadOnlyDoubleProperty progressProperty();
    ReadOnlyDoubleProperty fetchProgressProperty();

    void setAutoSkip(boolean autoSkip);

    void stopTask(Task<Void> task);

    void addNewLibrary();

    Task<Void> refreshLibraries();

    Task<Void> cleanupGames();

    Task<Void> processPath(Library library, Path path);
}
