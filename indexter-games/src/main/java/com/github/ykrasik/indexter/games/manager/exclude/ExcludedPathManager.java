package com.github.ykrasik.indexter.games.manager.exclude;

import com.github.ykrasik.indexter.games.datamodel.persistence.ExcludedPath;
import javafx.beans.property.ReadOnlyListProperty;

import java.nio.file.Path;
import java.util.List;

/**
 * @author Yevgeny Krasik
 */
public interface ExcludedPathManager {
    List<ExcludedPath> getAllExcludedPaths();

    ExcludedPath addExcludedPath(Path path);
    void deleteExcludedPath(ExcludedPath excludedPath);
    boolean isExcluded(Path path);

    ReadOnlyListProperty<ExcludedPath> excludedPathsProperty();
}
