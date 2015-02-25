package com.github.ykrasik.gamedex.core.ui.detail;

import com.github.ykrasik.gamedex.common.util.JavaFxUtils;
import com.github.ykrasik.gamedex.common.util.StringUtils;
import com.github.ykrasik.gamedex.core.service.image.ImageService;
import com.github.ykrasik.gamedex.core.ui.UIResources;
import com.github.ykrasik.gamedex.core.ui.rating.FixedRating;
import com.github.ykrasik.gamedex.datamodel.persistence.Game;
import com.github.ykrasik.gamedex.datamodel.persistence.Genre;
import com.github.ykrasik.opt.Opt;
import com.google.common.base.Strings;
import com.gs.collections.api.list.ImmutableList;
import com.gs.collections.api.tuple.Pair;
import com.gs.collections.impl.tuple.Tuples;
import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import lombok.NonNull;
import lombok.SneakyThrows;

import java.awt.*;
import java.net.URI;

import static com.github.ykrasik.gamedex.common.util.StringUtils.isUnavailable;
import static com.github.ykrasik.gamedex.common.util.StringUtils.toStringOrUnavailable;

/**
 * @author Yevgeny Krasik
 */
public class GameDetailScreen {
    private static final Duration FADE_DURATION = Duration.seconds(0.25);

    @FXML private GridPane posterContainer;
    @FXML private ImageView poster;

    @FXML private VBox attributesContainer;
    @FXML private Label nameLabel;
    @FXML private Label pathLabel;

    @FXML private TextArea description;
    @FXML private TextField platform;
    @FXML private TextField releaseDate;

    @FXML private HBox criticScoreContainer;
    @FXML private TextField criticScore;
    private FixedRating criticRating;

    @FXML private TextField userScore;
    @FXML private TextField genres;
    @FXML private Hyperlink url;

    private final ImageService imageService;
    private final Stage stage;
    private final BorderPane root;

    private Game inspectedGame;
    private Opt<Game> result = Opt.absent();

    public GameDetailScreen(@NonNull ImageService imageService) {
        this.imageService = imageService;

        final Pair<Stage, BorderPane> stageAndRoot = JavaFxUtils.returnLaterIfNecessary(this::createStageAndRoot);
        this.stage = stageAndRoot.getOne();
        this.root = stageAndRoot.getTwo();
    }

    @SneakyThrows
    private Pair<Stage, BorderPane> createStageAndRoot() {
        final Stage stage = new Stage();

        final FXMLLoader loader = new FXMLLoader(UIResources.gameDetailScreenFxml());
        loader.setController(this);
        final BorderPane root = loader.load();
        root.setId("gameDetailView");

        final Scene scene = new Scene(root, Color.TRANSPARENT);
        scene.getStylesheets().addAll(UIResources.mainCss(), UIResources.gameDetailScreenCss());

        stage.setMaximized(true);
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setScene(scene);

        return Tuples.pair(stage, root);
    }

    @FXML
    private void initialize() {
        final Rectangle2D bounds = Screen.getPrimary().getBounds();
        final double screenWidth = bounds.getWidth();

        posterContainer.setMinWidth(screenWidth * 0.3);
        posterContainer.setMaxWidth(screenWidth * 0.7);

        poster.imageProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.getHeight() > newValue.getWidth()) {
                poster.setFitWidth(0);
                poster.setFitHeight(Math.min(posterContainer.getHeight(), newValue.getHeight() * 2));
            } else {
                poster.setFitWidth(Math.min(posterContainer.getWidth(), newValue.getWidth() * 2));
                poster.setFitHeight(0);
            }
        });

        attributesContainer.setMaxWidth(screenWidth * 0.6);
        nameLabel.setMaxWidth(screenWidth * 0.5);
        pathLabel.setMaxWidth(screenWidth * 0.5);

        criticRating = new FixedRating(10);
        criticRating.setPartialRating(true);
        criticScoreContainer.getChildren().add(criticRating);
    }

    public Opt<Game> show(Game game) {
        inspectedGame = game;

        imageService.fetchPoster(game.getId(), poster);

        nameLabel.setText(game.getName());
        pathLabel.setText(game.getPath().toString());
        description.setText(toStringOrUnavailable(game.getDescription()));
        platform.setText(game.getPlatform().name());
        releaseDate.setText(toStringOrUnavailable(game.getReleaseDate()));
        criticScore.setText(toStringOrUnavailable(game.getCriticScore()));
        criticRating.setRating(game.getCriticScore().getOrElse(0.0) / 10);
        userScore.setText(toStringOrUnavailable(game.getUserScore()));
        genres.setText(StringUtils.toPrettyCsv(game.getGenres().collect(Genre::getName)));

        url.setText(game.getMetacriticDetailUrl());
        url.setVisited(false);
        url.setOnAction(e -> browseToUrl(game.getMetacriticDetailUrl()));

        doShow();
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

    private void doShow() {
        final FadeTransition fade = new FadeTransition(FADE_DURATION, root);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();

        stage.showAndWait();
    }

    @FXML
    public void accept() {
        result = Opt.of(createFromInput());
        close();
    }

    @FXML
    public void reject() {
        result = Opt.absent();
        close();
    }

    private void close() {
        final FadeTransition fade = new FadeTransition(FADE_DURATION, root);
        fade.setFromValue(1);
        fade.setToValue(0);
        fade.setOnFinished(e -> stage.hide());
        fade.play();
    }
}
