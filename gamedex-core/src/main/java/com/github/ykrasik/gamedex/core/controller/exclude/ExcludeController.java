package com.github.ykrasik.gamedex.core.controller.exclude;

import com.github.ykrasik.gamedex.core.controller.Controller;
import com.github.ykrasik.gamedex.core.manager.exclude.ExcludedPathManager;
import com.github.ykrasik.gamedex.core.service.action.ActionService;
import com.github.ykrasik.gamedex.datamodel.persistence.ExcludedPath;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * @author Yevgeny Krasik
 */
@RequiredArgsConstructor
public class ExcludeController implements Controller {
    @FXML private ListView<ExcludedPath> excludedPathsList;

    @NonNull private final ActionService actionService;
    @NonNull private final ExcludedPathManager excludedPathManager;

    @FXML
    public void initialize() {
        excludedPathsList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        excludedPathsList.itemsProperty().bind(excludedPathManager.excludedPathsProperty());
    }

    @FXML
    public void addExcludedPath() {
        actionService.addNewExcludedPath();
    }

    @FXML
    public void deleteExcludedPaths() {
        actionService.deleteExcludedPaths(excludedPathsList.getSelectionModel().getSelectedItems());
    }
}
