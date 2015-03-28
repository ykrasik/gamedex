package com.github.ykrasik.gamedex.core.controller.library;

import com.github.ykrasik.gamedex.core.controller.Controller;
import com.github.ykrasik.gamedex.core.manager.library.LibraryManager;
import com.github.ykrasik.gamedex.core.service.action.ActionService;
import com.github.ykrasik.gamedex.datamodel.persistence.Library;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * @author Yevgeny Krasik
 */
@RequiredArgsConstructor
public class LibraryController implements Controller {
    @FXML private TableView<Library> libraryList;
    @FXML private TableColumn<Library, String> libraryName;
    @FXML private TableColumn<Library, String> libraryPlatform;
    @FXML private TableColumn<Library, String> libraryPath;

    @NonNull private final ActionService actionService;
    @NonNull private final LibraryManager libraryManager;

    @FXML
    public void initialize() {
        libraryName.setCellValueFactory(e -> new SimpleStringProperty(e.getValue().getName()));
        libraryPlatform.setCellValueFactory(e -> new SimpleStringProperty(e.getValue().getPlatform().toString()));
        libraryPath.setCellValueFactory(e -> new SimpleStringProperty(e.getValue().getPath().toString()));

        libraryList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        libraryList.itemsProperty().bind(libraryManager.librariesProperty());
    }

    @FXML
    public void addLibrary() {
        actionService.addNewLibrary();
    }

    @FXML
    public void deleteLibrary() {
        actionService.deleteLibraries(libraryList.getSelectionModel().getSelectedItems());
    }
}