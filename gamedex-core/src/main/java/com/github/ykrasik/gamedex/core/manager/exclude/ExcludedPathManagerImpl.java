package com.github.ykrasik.gamedex.core.manager.exclude;

import com.github.ykrasik.gamedex.common.service.AbstractService;
import com.github.ykrasik.gamedex.common.util.JavaFxUtils;
import com.github.ykrasik.gamedex.datamodel.persistence.ExcludedPath;
import com.github.ykrasik.gamedex.persistence.PersistenceService;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ReadOnlyListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.nio.file.Path;
import java.util.List;

/**
 * @author Yevgeny Krasik
 */
@RequiredArgsConstructor
public class ExcludedPathManagerImpl extends AbstractService implements ExcludedPathManager {
    @NonNull private final PersistenceService persistenceService;

    private final ListProperty<ExcludedPath> excludedPathsProperty = new SimpleListProperty<>();
    private ObservableList<ExcludedPath> excludedPaths = FXCollections.emptyObservableList();

    @Override
    protected void doStart() throws Exception {
        LOG.info("Loading excluded paths...");
        excludedPaths = FXCollections.observableArrayList(persistenceService.getAllExcludedPaths().castToList());
        excludedPathsProperty.setValue(excludedPaths);
        LOG.info("Excluded Paths: {}", excludedPaths.size());
    }

    @Override
    protected void doStop() throws Exception {

    }

    @Override
    public List<ExcludedPath> getAllExcludedPaths() {
        return excludedPaths;
    }

    @Override
    public ExcludedPath addExcludedPath(Path path) {
        final ExcludedPath excludedPath = persistenceService.addExcludedPath(path);
        LOG.info("Excluded path: {}", excludedPath);

        // Update cache.
        JavaFxUtils.runLaterIfNecessary(() -> excludedPaths.add(excludedPath));
        return excludedPath;
    }

    @Override
    public void deleteExcludedPath(ExcludedPath excludedPath) {
        persistenceService.deleteExcludedPath(excludedPath.getId());
        LOG.info("Deleted excluded path: {}", excludedPath);

        // Delete from cache.
        JavaFxUtils.runLaterIfNecessary(() -> excludedPaths.remove(excludedPath));
    }

    @Override
    public boolean isExcluded(Path path) {
        return persistenceService.isPathExcluded(path);
    }

    @Override
    public ReadOnlyListProperty<ExcludedPath> excludedPathsProperty() {
        return excludedPathsProperty;
    }
}
