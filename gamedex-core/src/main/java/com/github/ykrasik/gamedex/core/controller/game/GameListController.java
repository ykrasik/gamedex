package com.github.ykrasik.gamedex.core.controller.game;

import com.github.ykrasik.gamedex.common.util.StringUtils;
import com.github.ykrasik.gamedex.core.controller.Controller;
import com.github.ykrasik.gamedex.core.manager.game.GameManager;
import com.github.ykrasik.gamedex.core.service.action.ActionService;
import com.github.ykrasik.gamedex.core.service.image.ImageService;
import com.github.ykrasik.gamedex.datamodel.persistence.Game;
import com.github.ykrasik.gamedex.datamodel.persistence.Genre;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import java.awt.*;
import java.net.URI;

import static com.github.ykrasik.gamedex.common.util.StringUtils.toStringOrUnavailable;

/**
 * @author Yevgeny Krasik
 */
@RequiredArgsConstructor
public class GameListController implements Controller {
    @FXML private TableView<Game> gameList;
    @FXML private TableColumn<Game, String> gameNameColumn;
    @FXML private TableColumn<Game, String> gamePlatformColumn;
    @FXML private TableColumn<Game, String> gameReleaseDateColumn;
    @FXML private TableColumn<Game, Number> gameCriticScoreColumn;
    @FXML private TableColumn<Game, Number> gameUserScoreColumn;
    @FXML private TableColumn<Game, String> gamePathColumn;
    @FXML private TableColumn<Game, String> gameDateAddedColumn;

    @FXML private GridPane sideBarContainer;
    @FXML private ImageView poster;
    @FXML private GridPane attributes;
    @FXML private TextField gamePathTextField;
    @FXML private TextField nameTextField;
    @FXML private TextArea descriptionTextArea;
    @FXML private TextField platformTextField;
    @FXML private TextField releaseDateTextField;
    @FXML private TextField criticScoreTextField;
    @FXML private TextField userScoreTextField;
    @FXML private TextField genresTextField;
    @FXML private Hyperlink urlHyperlink;

    @NonNull private final ImageService imageService;
    @NonNull private final ActionService actionService;
    @NonNull private final GameManager gameManager;

    @FXML
    private void initialize() {
        initGameList();
        initSideBar();
    }

    private void initGameList() {
        gameNameColumn.setCellValueFactory(e -> new SimpleStringProperty(e.getValue().getName()));
        gamePlatformColumn.setCellValueFactory(e -> new SimpleStringProperty(e.getValue().getPlatform().toString()));
        gameReleaseDateColumn.setCellValueFactory(e -> new SimpleStringProperty(toStringOrUnavailable(e.getValue().getReleaseDate())));
        gameCriticScoreColumn.setCellValueFactory(e -> new SimpleDoubleProperty(e.getValue().getCriticScore().getOrElse(0.0)));
        gameUserScoreColumn.setCellValueFactory(e -> new SimpleDoubleProperty(e.getValue().getUserScore().getOrElse(0.0)));
        gamePathColumn.setCellValueFactory(e -> new SimpleStringProperty(e.getValue().getPath().toString()));
        gameDateAddedColumn.setCellValueFactory(e -> new SimpleStringProperty(e.getValue().getLastModified().toLocalDate().toString()));

        gameList.itemsProperty().bind(gameManager.gamesProperty());
        gameList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                displayGame(newValue);
            }
        });
    }

    private void displayGame(Game game) {
        imageService.fetchPoster(game.getId(), poster);

        gamePathTextField.setText(game.getPath().toString());
        nameTextField.setText(game.getName());
        descriptionTextArea.setText(toStringOrUnavailable(game.getDescription()));
        platformTextField.setText(game.getPlatform().name());
        releaseDateTextField.setText(toStringOrUnavailable(game.getReleaseDate()));
        criticScoreTextField.setText(toStringOrUnavailable(game.getCriticScore()));
        userScoreTextField.setText(toStringOrUnavailable(game.getUserScore()));
        genresTextField.setText(StringUtils.toPrettyCsv(game.getGenres().collect(Genre::getName)));

        urlHyperlink.setText(game.getMetacriticDetailUrl());
        urlHyperlink.setVisited(false);
        urlHyperlink.setOnAction(e -> browseToUrl(game.getMetacriticDetailUrl()));
    }

    private void initSideBar() {
        // Make the poster resize dynamically according to how much space is available, minus margins.
        // TODO: If image is small, have the description take up that space.
        poster.fitWidthProperty().bind(sideBarContainer.widthProperty().subtract(10));
        poster.fitHeightProperty().bind(sideBarContainer.heightProperty().subtract(attributes.heightProperty()).subtract(13));
    }

    @FXML
    private void deleteGame() {
        final Game game = gameList.getSelectionModel().getSelectedItem();
        actionService.deleteGame(game);
    }

    @SneakyThrows
    private void browseToUrl(String url) {
        Desktop.getDesktop().browse(new URI(url));
    }

    public void selectGame(Game game) {
        gameList.getSelectionModel().select(game);
    }
}
