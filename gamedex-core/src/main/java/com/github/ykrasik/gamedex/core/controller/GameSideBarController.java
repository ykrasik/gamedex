package com.github.ykrasik.gamedex.core.controller;

import com.github.ykrasik.gamedex.common.util.ListUtils;
import com.github.ykrasik.gamedex.core.ui.UIResources;
import com.github.ykrasik.gamedex.datamodel.ImageData;
import com.github.ykrasik.gamedex.datamodel.persistence.Game;
import com.github.ykrasik.gamedex.datamodel.persistence.Genre;
import com.google.common.base.Joiner;
import javafx.fxml.FXML;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import lombok.SneakyThrows;

import java.awt.*;
import java.net.URI;

import static com.github.ykrasik.gamedex.common.util.StringUtils.toStringOrUnavailable;

/**
 * @author Yevgeny Krasik
 */
public class GameSideBarController implements Controller {
    private static final Joiner JOINER = Joiner.on(", ").skipNulls();

    @FXML private VBox sideBar;
    @FXML private ImageView poster;
    @FXML private TextField gamePath;
    @FXML private TextField name;
    @FXML private TextArea description;
    @FXML private TextField platform;
    @FXML private TextField releaseDate;
    @FXML private TextField criticScore;
    @FXML private TextField userScore;
    @FXML private TextField genres;
    @FXML private Hyperlink url;

    // Called by JavaFX
    public void initialize() {

    }

    public void displayGame(Game game) {
        poster.setImage(game.getPoster().orElse(game.getThumbnail()).map(ImageData::getImage).getOrElse(UIResources.getNotAvailable()));
        gamePath.setText(game.getPath().toString());
        name.setText(game.getName());
        description.setText(toStringOrUnavailable(game.getDescription()));
        platform.setText(game.getPlatform().name());
        releaseDate.setText(toStringOrUnavailable(game.getReleaseDate()));
        criticScore.setText(toStringOrUnavailable(game.getCriticScore()));
        userScore.setText(toStringOrUnavailable(game.getUserScore()));
        genres.setText(JOINER.join(ListUtils.map(game.getGenres(), Genre::getName)));

        url.setText(game.getMetacriticDetailUrl());
        url.setVisited(false);
        url.setOnAction(e -> browseToUrl(game.getMetacriticDetailUrl()));
    }

    @SneakyThrows
    private void browseToUrl(String url) {
        Desktop.getDesktop().browse(new URI(url));
    }
}
