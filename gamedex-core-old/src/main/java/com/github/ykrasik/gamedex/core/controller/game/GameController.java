package com.github.ykrasik.gamedex.core.controller.game;

import com.github.ykrasik.gamedex.core.controller.Controller;
import com.github.ykrasik.gamedex.core.javafx.MoreBindings;
import com.github.ykrasik.gamedex.core.manager.game.GameManager;
import com.github.ykrasik.gamedex.core.manager.game.GameSort;
import com.github.ykrasik.gamedex.core.manager.library.LibraryManager;
import com.github.ykrasik.gamedex.core.service.action.ActionService;
import com.github.ykrasik.gamedex.core.service.config.ConfigService;
import com.github.ykrasik.gamedex.core.ui.dialog.GenreFilterDialog;
import com.github.ykrasik.gamedex.core.ui.library.LibraryFilterDialog;
import com.github.ykrasik.gamedex.core.util.StringUtils;
import com.github.ykrasik.gamedex.datamodel.Genre;
import com.github.ykrasik.gamedex.common.datamodel.Library;
import com.github.ykrasik.yava.option.Opt;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * @author Yevgeny Krasik
 */
@RequiredArgsConstructor
public class GameController implements Controller {
    @FXML private TextField gameSearchTextField;
    @FXML private Button clearGameSearchButton;

    @FXML private TextField filteredGenresTextField;
    @FXML private Button clearGenreFilterButton;
    private final ObjectProperty<ObservableList<Genre>> currentlyFilteredGenres = new SimpleObjectProperty<>(FXCollections.emptyObservableList());

    @FXML private TextField filteredLibraryTextField;
    @FXML private Button clearLibraryFilterButton;
    private final ObjectProperty<Library> currentlyFilteredLibrary = new SimpleObjectProperty<>();

    @FXML private ComboBox<GameSort> gameSortComboBox;

    @FXML private CheckBox autoSkipCheckBox;

    @FXML private GameWallController gameWallController;
    @FXML private GameListController gameListController;

    private final ObjectProperty<Task<Void>> currentTaskProperty = new SimpleObjectProperty<>();

    @NonNull private final ConfigService configService;
    @NonNull private final ActionService actionService;
    @NonNull private final GameManager gameManager;
    @NonNull private final LibraryManager libraryManager;

    @FXML
    private void initialize() {
        initGameWall();
        initGameSearch();
        initGenreFilter();
        initLibraryFilter();
        initGameSort();
        initAutoSkip();
    }

    private void initGameWall() {
        gameWallController.selectedGameProperty().addListener((observable, oldValue, newValue) -> {
            gameListController.selectGame(newValue);
        });
    }

    private void initGameSearch() {
        gameSearchTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.isEmpty()) {
                gameManager.noNameFilter();
            } else {
                gameManager.nameFilter(newValue);
            }
        });

        clearGameSearchButton.visibleProperty().bind(Bindings.isNotEmpty(gameSearchTextField.textProperty()));
        clearGameSearchButton.setOnAction(e -> gameSearchTextField.clear());
    }

    private void initGenreFilter() {
        currentlyFilteredGenres.addListener((observable, oldValue, newValue) -> {
            if (newValue.isEmpty()) {
                gameManager.noGenreFilter();
                filteredGenresTextField.setText("");
            } else {
                gameManager.genreFilter(newValue);
                filteredGenresTextField.setText(StringUtils.toPrettyCsv(newValue));
            }
        });

        clearGenreFilterButton.visibleProperty().bind(MoreBindings.isNotEmpty(currentlyFilteredGenres));
        clearGenreFilterButton.setOnAction(e -> currentlyFilteredGenres.set(FXCollections.emptyObservableList()));
    }

    private void initLibraryFilter() {
        currentlyFilteredLibrary.addListener((observable, oldValue, newValue) -> {
            if (newValue == null) {
                gameManager.noLibraryFilter();
                filteredLibraryTextField.setText("");
            } else {
                gameManager.libraryFilter(newValue);
                filteredLibraryTextField.setText(newValue.getName());
            }
        });

        clearLibraryFilterButton.visibleProperty().bind(Bindings.isNotNull(currentlyFilteredLibrary));
        clearLibraryFilterButton.setOnAction(e -> currentlyFilteredLibrary.set(null));
    }

    private void initGameSort() {
        gameSortComboBox.setItems(FXCollections.observableArrayList(GameSort.values()));
        gameSortComboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            gameManager.sort(newValue);
        });
        gameSortComboBox.setValue(configService.getGameSort());
        configService.gameSortProperty().bind(gameSortComboBox.getSelectionModel().selectedItemProperty());
    }

    private void initAutoSkip() {
        autoSkipCheckBox.setSelected(configService.isAutoSkip());
        configService.autoSkipProperty().bind(autoSkipCheckBox.selectedProperty());
    }

    @FXML
    private void filterGenres() {
        // Sort genres by name.
        final ObservableList<Genre> genres = gameManager.getAllGenres();
        FXCollections.sort(genres);

        final Opt<List<Genre>> selectedGenres = new GenreFilterDialog()
            .previouslyCheckedItems(currentlyFilteredGenres.get())
            .show(genres);
        selectedGenres.ifDefined(selected -> {
            if (!selected.isEmpty()) {
                currentlyFilteredGenres.set(FXCollections.observableArrayList(selected));
            }
        });
    }

    @FXML
    private void filterLibrary() {
        // Sort libraries by name.
        final ObservableList<Library> libraries = FXCollections.observableArrayList(libraryManager.getAllLibraries());
        FXCollections.sort(libraries);

        final Opt<Library> selectedLibrary = new LibraryFilterDialog()
            .previouslySelectedItem(currentlyFilteredLibrary.get())
            .show(libraries);
        if (selectedLibrary.isDefined()) {
            currentlyFilteredLibrary.setValue(selectedLibrary.get());
        }
    }

    @FXML
    private void refreshLibraries() {
        registerCurrentTask(actionService.refreshLibraries());
    }

    private void registerCurrentTask(Task<Void> task) {
        currentTaskProperty.set(task);
    }

    public ReadOnlyObjectProperty<Task<Void>> currentTaskProperty() {
        return currentTaskProperty;
    }
}
