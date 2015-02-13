package com.github.ykrasik.gamedex.core.controller;

import com.github.ykrasik.gamedex.core.exclude.ExcludedPathManager;
import com.github.ykrasik.gamedex.datamodel.persistence.ExcludedPath;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * @author Yevgeny Krasik
 */
@RequiredArgsConstructor
public class ExcludedController implements Controller {
    @FXML private ListView<ExcludedPath> excludedPathsList;

    @NonNull private final ExcludedPathManager excludedPathManager;

    // Called by JavaFX
    public void initialize() {
        excludedPathsList.itemsProperty().bind(excludedPathManager.excludedPathsProperty());
    }
}
