package com.github.ykrasik.gamedex.core.service.action;

import com.github.ykrasik.gamedex.core.manager.Messageable;
import com.github.ykrasik.gamedex.datamodel.ExcludedPath;
import com.github.ykrasik.gamedex.datamodel.Game;
import com.github.ykrasik.gamedex.common.datamodel.Library;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;

import java.nio.file.Path;

/**
 * @author Yevgeny Krasik
 */
// TODO: Everything should be done in this class, the controller should simply delegate here.
// TODO: This class should sub-divide responsibilities among the rest of the classes.
// TODO: Rename this to ActionManager? Each flow returns a Result class with properties?
// TODO: Need 2 threads - one to search and fetch games, one to fetch thumbnails & posters.
public interface ActionService extends Messageable {
    ReadOnlyDoubleProperty progressProperty();
    ReadOnlyBooleanProperty fetchingProperty();

    void stopTask(Task<Void> task);

    void addNewLibrary();

    void addNewExcludedPath();

    Task<Void> refreshLibraries();

    Task<Void> cleanup();

    Task<Void> processPath(Library library, Path path);

    void deleteGame(Game game);

    void deleteExcludedPaths(ObservableList<ExcludedPath> excludedPaths);

    void deleteLibraries(ObservableList<Library> libraries);
}
