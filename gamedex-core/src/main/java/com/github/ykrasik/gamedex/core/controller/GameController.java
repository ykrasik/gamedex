package com.github.ykrasik.gamedex.core.controller;

import com.github.ykrasik.gamedex.common.util.StringUtils;
import com.github.ykrasik.gamedex.core.flow.FlowManager;
import com.github.ykrasik.gamedex.core.game.GameManager;
import com.github.ykrasik.gamedex.core.game.GameSort;
import com.github.ykrasik.gamedex.core.library.LibraryManager;
import com.github.ykrasik.gamedex.core.ui.dialog.GenreFilterDialog;
import com.github.ykrasik.gamedex.core.ui.library.LibraryFilterDialog;
import com.github.ykrasik.gamedex.datamodel.persistence.Genre;
import com.github.ykrasik.gamedex.datamodel.persistence.Library;
import com.github.ykrasik.opt.Opt;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
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

    @FXML private Button filterGenreButton;
    @FXML private TextField filteredGenresTextField;
    @FXML private Button clearGenreFilterButton;
    private final ObjectProperty<ObservableList<Genre>> currentlyFilteredGenres = new SimpleObjectProperty<>(FXCollections.emptyObservableList());

    @FXML private Button filterLibraryButton;
    @FXML private TextField filteredLibraryTextField;
    @FXML private Button clearLibraryFilterButton;
    private final ObjectProperty<Library> currentlyFilteredLibrary = new SimpleObjectProperty<>();

    @FXML private ComboBox<GameSort> gameSort;

    @FXML private CheckBox autoSkipCheckBox;
    @FXML private Button refreshLibrariesButton;

    @FXML private SplitPane content;

    @FXML private GameWallController gameWallController;
    @FXML private GameListController gameListController;
    @FXML private GameSideBarController gameSideBarController;

    @NonNull private FlowManager flowManager;
    @NonNull private GameManager gameManager;
    @NonNull private LibraryManager libraryManager;

    // Called by JavaFX
    public void initialize() {
        initGameWall();
        initGameList();
        initGameSearch();
        initGenreFilter();
        initLibraryFilter();
        initGameSort();
        initRefreshLibraries();
    }

    // TODO: Interface?
    private void initGameWall() {
        // TODO: gameWall has a problem refreshing... so instead of binding, add a listener and clear the wall before setting the value.
        gameWallController.itemsProperty().bind(gameManager.gamesProperty());
        gameWallController.selectedGameProperty().addListener((observable, oldValue, newValue) -> {
            gameSideBarController.displayGame(newValue);
        });
        gameWallController.deletedGameProperty().addListener((observable, oldValue, newValue) -> {
            gameManager.deleteGame(newValue);
        });
    }

    private void initGameList() {
        gameListController.itemsProperty().bind(gameManager.gamesProperty());
        gameListController.selectedGameProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                gameSideBarController.displayGame(newValue);
            }
        });
        gameListController.deletedGameProperty().addListener((observable, oldValue, newValue) -> {
            gameManager.deleteGame(newValue);
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

        filterGenreButton.setOnAction(e -> {
            // Sort genres by name.
            final ObservableList<Genre> genres = gameManager.getAllGenres();
            FXCollections.sort(genres);

            final Opt<List<Genre>> selectedGenres = new GenreFilterDialog()
                .previouslyCheckedItems(currentlyFilteredGenres.get())
                .show(genres);
            selectedGenres.ifPresent(selected -> {
                if (!selected.isEmpty()) {
                    currentlyFilteredGenres.set(FXCollections.observableArrayList(selected));
                }
            });
        });

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

        filterLibraryButton.setOnAction(e -> {
            // Sort libraries by name.
            final ObservableList<Library> libraries = FXCollections.observableArrayList(libraryManager.getAllLibraries());
            FXCollections.sort(libraries);

            final Opt<Library> selectedLibrary = new LibraryFilterDialog()
                .previouslySelectedItem(currentlyFilteredLibrary.get())
                .show(libraries);
            if (selectedLibrary.isPresent()) {
                currentlyFilteredLibrary.setValue(selectedLibrary.get());
            }
        });

        clearLibraryFilterButton.setOnAction(e -> currentlyFilteredLibrary.set(null));
    }

    private void initGameSort() {
        gameSort.setItems(FXCollections.observableArrayList(GameSort.values()));
        gameSort.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            gameManager.sort(newValue);
        });
        gameSort.setValue(GameSort.NAME_ASC);
    }

    private void initRefreshLibraries() {
        autoSkipCheckBox.selectedProperty().addListener(e -> flowManager.setAutoSkip(autoSkipCheckBox.isSelected()));
        refreshLibrariesButton.setOnAction(e -> flowManager.refreshLibraries());
    }
}
