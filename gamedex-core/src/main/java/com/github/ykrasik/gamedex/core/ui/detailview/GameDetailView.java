package com.github.ykrasik.gamedex.core.ui.detailview;

import com.github.ykrasik.gamedex.common.util.StringUtils;
import com.github.ykrasik.gamedex.core.service.image.ImageService;
import com.github.ykrasik.gamedex.core.ui.UIResources;
import com.github.ykrasik.gamedex.datamodel.persistence.Game;
import com.github.ykrasik.gamedex.datamodel.persistence.Genre;
import com.github.ykrasik.opt.Opt;
import com.google.common.base.Strings;
import com.gs.collections.api.list.ImmutableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.*;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import lombok.NonNull;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;

import java.awt.*;
import java.net.URI;

import static com.github.ykrasik.gamedex.common.util.StringUtils.isUnavailable;
import static com.github.ykrasik.gamedex.common.util.StringUtils.toStringOrUnavailable;

/**
 * @author Yevgeny Krasik
 */
@Accessors(fluent = true)
public class GameDetailView {
    private static final GameDetailView INSTANCE = new GameDetailView();

    private final Stage stage = new Stage();

    @FXML private GridPane posterContainer;
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

    @FXML private Button okButton;
    @FXML private Button cancelButton;

    // FIXME: Ugh...
    @Setter @NonNull private ImageService imageService;

    private Game inspectedGame;
    private Opt<Game> result = Opt.absent();

    @SneakyThrows
    private GameDetailView() {
        final FXMLLoader loader = new FXMLLoader(UIResources.detailViewDialogFxml());
        loader.setController(this);
        final BorderPane root = loader.load();
        root.setId("gameDetailView");

        final Scene scene = new Scene(root, Color.TRANSPARENT);
        scene.getStylesheets().addAll(UIResources.mainCss(), UIResources.detailViewDialogCss());

        poster.fitWidthProperty().bind(posterContainer.widthProperty());
        poster.fitHeightProperty().bind(posterContainer.heightProperty());

        okButton.setOnAction(e -> {
            result = Opt.of(createFromInput());
            stage.hide();
        });
        cancelButton.setOnAction(e -> {
            result = Opt.absent();
            stage.hide();
        });

        stage.setMaximized(true);
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setScene(scene);
    }

    public Opt<Game> show(Game game) {
        inspectedGame = game;

        imageService.fetchPoster(game.getId(), poster);

        gamePath.setText(game.getPath().toString());
        name.setText(game.getName());
        description.setText(toStringOrUnavailable(game.getDescription()));
        platform.setText(game.getPlatform().name());
        releaseDate.setText(toStringOrUnavailable(game.getReleaseDate()));
        criticScore.setText(toStringOrUnavailable(game.getCriticScore()));
        userScore.setText(toStringOrUnavailable(game.getUserScore()));
        genres.setText(StringUtils.toPrettyCsv(game.getGenres().collect(Genre::getName)));

        url.setText(game.getMetacriticDetailUrl());
        url.setVisited(false);
        url.setOnAction(e -> browseToUrl(game.getMetacriticDetailUrl()));

        stage.showAndWait();
        return result;
    }

    @SneakyThrows
    private void browseToUrl(String url) {
        Desktop.getDesktop().browse(new URI(url));
    }

    private Game createFromInput() {
        return Game.builder()
            .id(inspectedGame.getId())
            .path(inspectedGame.getPath())
            .metacriticDetailUrl(inspectedGame.getMetacriticDetailUrl())
            .giantBombDetailUrl(inspectedGame.getGiantBombDetailUrl())
            .name(inspectedGame.getName())
            .platform(inspectedGame.getPlatform())
            .description(descriptionFromInput())
            .releaseDate(inspectedGame.getReleaseDate())
            .criticScore(criticScoreFromInput())
            .userScore(userScoreFromInput())
            .lastModified(inspectedGame.getLastModified())
            .genres(genresFromInput())
            .libraries(inspectedGame.getLibraries())
            .build();
    }

    private Opt<String> descriptionFromInput() {
        return readText(description);
    }

    private Opt<Double> criticScoreFromInput() {
        return readText(criticScore).map(Double::parseDouble);
    }

    private Opt<Double> userScoreFromInput() {
        return readText(userScore).map(Double::parseDouble);
    }

    private ImmutableList<Genre> genresFromInput() {
        // TODO: Implement
        return inspectedGame.getGenres();
    }

    private Opt<String> readText(TextInputControl control) {
        return Opt.ofNullable(Strings.emptyToNull(control.getText())).filter(text -> !isUnavailable(text));
    }

    public static GameDetailView create() {
        return INSTANCE;
    }
}
