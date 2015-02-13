package com.github.ykrasik.gamedex.core.controller;

import com.github.ykrasik.gamedex.core.game.GameManager;
import com.github.ykrasik.gamedex.core.game.GameSort;
import com.github.ykrasik.gamedex.core.ui.dialog.SearchableCheckListViewDialog;
import com.github.ykrasik.gamedex.datamodel.persistence.Genre;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;

/**
 * @author Yevgeny Krasik
 */
@RequiredArgsConstructor
public class GameController implements Controller {
    @FXML private TextField gameSearch;
    @FXML private Button clearGameSearch;
    @FXML private Button filterGenreButton;
    @FXML private ComboBox<String> gameSort;

    @FXML private HBox contentScreen;

    @FXML private GameWallController gameWallController;
    @FXML private GameListController gameListController;
    @FXML private GameSideBarController gameSideBarController;

    @NonNull GameManager gameManager;

    // Called by JavaFX
    public void initialize() {
        initGameWall();
        initGameList();
        initGameSearch();
        initGenreFilter();
        initGameSort();
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
            gameSideBarController.displayGame(newValue);
        });
        gameListController.deletedGameProperty().addListener((observable, oldValue, newValue) -> {
            gameManager.deleteGame(newValue);
        });
    }

    private void initGameSearch() {
        gameSearch.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.isEmpty()) {
                gameManager.noNameFilter();
            } else {
                gameManager.nameFilter(newValue);
            }
        });

        clearGameSearch.setOnAction(e -> gameSearch.clear());
    }

    private void initGenreFilter() {
        filterGenreButton.setOnAction(e -> {
            final Optional<List<Genre>> selectedGenres = new SearchableCheckListViewDialog<Genre>()
                .title("Select Genres:")
                .show(gameManager.getAllGenres());

            selectedGenres.ifPresent(genres -> {
                if (genres.isEmpty()) {
                    gameManager.noGenreFilter();
                } else {
                    gameManager.genreFilter(genres);
                }
            });
        });
    }

    private void initGameSort() {
        gameSort.setItems(FXCollections.observableArrayList(GameSort.getKeys()));
        gameSort.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            final GameSort sort = GameSort.fromString(newValue);
            if (sort == null) {
                throw new IllegalArgumentException("Invalid sort: " + newValue);
            }
            gameManager.sort(sort);
        });
        gameSort.setValue(GameSort.NAME.getKey());
    }
}
