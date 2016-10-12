package com.github.ykrasik.gamedex.core.manager.path;

import com.github.ykrasik.gamedex.core.flow.LibraryHierarchy;
import com.github.ykrasik.gamedex.core.manager.Messageable;
import javafx.beans.property.ReadOnlyBooleanProperty;

import java.nio.file.Path;

/**
 * @author Yevgeny Krasik
 */
public interface PathManager extends Messageable {
    ReadOnlyBooleanProperty fetchingProperty();

    ProcessPathReturnValue processPath(LibraryHierarchy libraryHierarchy, Path path) throws Exception;
}
