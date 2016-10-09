package com.github.ykrasik.gamedex.core.manager.exclude;

import com.github.ykrasik.gamedex.datamodel.ExcludedPath;
import javafx.beans.property.ReadOnlyListProperty;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

/**
 * @author Yevgeny Krasik
 */
public interface ExcludedPathManager {
    List<ExcludedPath> getAllExcludedPaths();

    ExcludedPath addExcludedPath(Path path);

    void deleteExcludedPath(ExcludedPath excludedPath);
    void deleteExcludedPaths(Collection<ExcludedPath> excludedPaths);

    boolean isExcluded(Path path);

    ReadOnlyListProperty<ExcludedPath> excludedPathsProperty();
}
