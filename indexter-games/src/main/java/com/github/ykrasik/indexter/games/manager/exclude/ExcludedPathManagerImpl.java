package com.github.ykrasik.indexter.games.manager.exclude;

import com.github.ykrasik.indexter.AbstractService;
import com.github.ykrasik.indexter.games.datamodel.persistence.ExcludedPath;
import com.github.ykrasik.indexter.games.persistence.PersistenceService;
import com.github.ykrasik.indexter.util.PlatformUtils;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.util.List;

/**
 * @author Yevgeny Krasik
 */
@Slf4j
@RequiredArgsConstructor
public class ExcludedPathManagerImpl extends AbstractService implements ExcludedPathManager {
    @NonNull private final PersistenceService persistenceService;

    private final ObjectProperty<ObservableList<ExcludedPath>> excludedPathsProperty = new SimpleObjectProperty<>();
    private ObservableList<ExcludedPath> excludedPaths = FXCollections.emptyObservableList();

    @Override
    protected void doStart() throws Exception {
        excludedPaths = FXCollections.observableArrayList(persistenceService.getAllExcludedPaths());
        excludedPathsProperty.setValue(excludedPaths);
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
        log.info("Excluded path: {}", excludedPath);

        // Update cache.
        PlatformUtils.runLaterIfNecessary(() -> excludedPaths.add(excludedPath));
        return excludedPath;
    }

    @Override
    public void deleteExcludedPath(ExcludedPath excludedPath) {
        persistenceService.deleteExcludedPath(excludedPath.getId());
        log.info("Deleted excluded path: {}", excludedPath);

        // Delete from cache.
        PlatformUtils.runLaterIfNecessary(() -> excludedPaths.remove(excludedPath));
    }

    @Override
    public boolean isExcluded(Path path) {
        return persistenceService.isPathExcluded(path);
    }

    @Override
    public ReadOnlyProperty<ObservableList<ExcludedPath>> excludedPathsProperty() {
        return excludedPathsProperty;
    }
}
