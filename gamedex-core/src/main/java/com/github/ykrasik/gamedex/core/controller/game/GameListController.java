package com.github.ykrasik.gamedex.core.controller.game;

import com.github.ykrasik.gamedex.core.controller.Controller;
import com.github.ykrasik.gamedex.datamodel.persistence.Game;
import javafx.beans.property.*;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import static com.github.ykrasik.gamedex.common.util.StringUtils.toStringOrUnavailable;

/**
 * @author Yevgeny Krasik
 */
public class GameListController implements Controller {
    @FXML private TableView<Game> gameList;
    @FXML private TableColumn<Game, String> gameNameColumn;
    @FXML private TableColumn<Game, String> gamePlatformColumn;
    @FXML private TableColumn<Game, String> gameReleaseDateColumn;
    @FXML private TableColumn<Game, Number> gameCriticScoreColumn;
    @FXML private TableColumn<Game, Number> gameUserScoreColumn;
    @FXML private TableColumn<Game, String> gamePathColumn;
    @FXML private TableColumn<Game, String> gameDateAddedColumn;

    @FXML private GameSideBarController gameSideBarController;

    // TODO: Add support.
    private final ObjectProperty<Game> deletedGameProperty = new SimpleObjectProperty<>();

    public ObjectProperty<ObservableList<Game>> itemsProperty() {
        return gameList.itemsProperty();
    }

    public ReadOnlyObjectProperty<Game> selectedGameProperty() {
        return gameList.getSelectionModel().selectedItemProperty();
    }

    public ReadOnlyObjectProperty<Game> deletedGameProperty() {
        return deletedGameProperty;
    }

    // Called by JavaFX
    public void initialize() {
        gameNameColumn.setCellValueFactory(e -> new SimpleStringProperty(e.getValue().getName()));
        gamePlatformColumn.setCellValueFactory(e -> new SimpleStringProperty(e.getValue().getPlatform().toString()));
        gameReleaseDateColumn.setCellValueFactory(e -> new SimpleStringProperty(toStringOrUnavailable(e.getValue().getReleaseDate())));
        gameCriticScoreColumn.setCellValueFactory(e -> new SimpleDoubleProperty(e.getValue().getCriticScore().getOrElse(0.0)));
        gameUserScoreColumn.setCellValueFactory(e -> new SimpleDoubleProperty(e.getValue().getUserScore().getOrElse(0.0)));
        gamePathColumn.setCellValueFactory(e -> new SimpleStringProperty(e.getValue().getPath().toString()));
        gameDateAddedColumn.setCellValueFactory(e -> new SimpleStringProperty(e.getValue().getLastModified().toLocalDate().toString()));

        selectedGameProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                gameSideBarController.displayGame(newValue);
            }
        });
    }

    public void selectGame(Game game) {
        gameList.getSelectionModel().select(game);
    }
}
