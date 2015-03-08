package com.github.ykrasik.gamedex.core.service.screen.detail;

import com.github.ykrasik.gamedex.common.util.StringUtils;
import com.github.ykrasik.gamedex.common.util.UrlUtils;
import com.github.ykrasik.gamedex.core.javafx.layout.ImageViewResizingPane;
import com.github.ykrasik.gamedex.core.manager.stage.StageManager;
import com.github.ykrasik.gamedex.core.service.action.ActionService;
import com.github.ykrasik.gamedex.core.service.image.ImageService;
import com.github.ykrasik.gamedex.core.ui.UIResources;
import com.github.ykrasik.gamedex.core.ui.rating.FixedRating;
import com.github.ykrasik.gamedex.datamodel.persistence.Game;
import com.github.ykrasik.gamedex.datamodel.persistence.Genre;
import com.github.ykrasik.opt.Opt;
import com.google.common.base.Strings;
import com.gs.collections.api.list.ImmutableList;
import javafx.animation.FadeTransition;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import lombok.NonNull;
import lombok.SneakyThrows;

import static com.github.ykrasik.gamedex.common.util.StringUtils.isUnavailable;
import static com.github.ykrasik.gamedex.common.util.StringUtils.toStringOrUnavailable;

/**
 * @author Yevgeny Krasik
 */
public class GameDetailsScreen {
    private static final Duration FADE_DURATION = Duration.seconds(0.25);
    private static final double MAX_POSTER_WIDTH_PERCENT = 0.5;

    @FXML private StackPane posterContainer;
    private ImageView poster;

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
    private final ActionService actionService;
    private final StageManager stageManager;

    private final Stage stage;
    private final BorderPane root;

    private Game game;
    private Opt<Game> result = Opt.absent();

    @SneakyThrows
    public GameDetailsScreen(@NonNull ImageService imageService,
                             @NonNull ActionService actionService,
                             @NonNull StageManager stageManager) {
        this.imageService = imageService;
        this.actionService = actionService;
        this.stageManager = stageManager;

        final Rectangle2D bounds = Screen.getPrimary().getBounds();

        final FXMLLoader loader = new FXMLLoader(UIResources.gameDetailsScreenFxml());
        loader.setController(this);
        root = loader.load();
        root.setId("gameDetailView");
        root.setMaxHeight(bounds.getHeight());
        root.setMaxWidth(bounds.getWidth());

        final Scene scene = new Scene(root, Color.TRANSPARENT);
        scene.getStylesheets().addAll(UIResources.mainCss(), UIResources.gameDetailsScreenCss());

        stage = new Stage();
        stage.setTitle("Details");
        stage.setMaxHeight(bounds.getHeight());
        stage.setMaxWidth(bounds.getWidth());
        stage.setMaximized(true);
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setScene(scene);
    }

    @FXML
    private void initialize() {
        final double screenWidth = Screen.getPrimary().getBounds().getWidth();

        poster = new ImageView();
        final ImageViewResizingPane posterPane = new ImageViewResizingPane(poster);
        posterPane.setMaxWidth(screenWidth * MAX_POSTER_WIDTH_PERCENT);

        // Clip the posterPane's corners to be round after the posterPane's size is calculated.
        final Rectangle clip = new Rectangle();
        clip.setArcWidth(20);
        clip.setArcHeight(20);
        final ChangeListener<Number> clipListener = (observable, oldValue, newValue) -> {
            clip.setWidth(posterPane.getWidth());
            clip.setHeight(posterPane.getHeight());
        };
        posterPane.heightProperty().addListener(clipListener);
        posterPane.widthProperty().addListener(clipListener);
        posterPane.setClip(clip);

        posterContainer.getChildren().add(posterPane);

        criticRating = new FixedRating(10);
        criticRating.setPartialRating(true);
        criticScoreContainer.getChildren().add(criticRating);
    }

    public void show(Game game) {
        this.game = game;

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
        url.setOnAction(e -> UrlUtils.browseToUrl(game.getMetacriticDetailUrl()));

        doShow();
        // FIXME: Handle exception while editing
        if (result.isPresent()) {
            // TODO: Update in db
            System.out.println(result);
        }
    }

    private Game createFromInput() {
        return Game.builder()
            .id(game.getId())
            .path(game.getPath())
            .metacriticDetailUrl(game.getMetacriticDetailUrl())
            .giantBombDetailUrl(game.getGiantBombDetailUrl())
            .name(game.getName())
            .platform(game.getPlatform())
            .description(descriptionFromInput())
            .releaseDate(game.getReleaseDate())
            .criticScore(criticScoreFromInput())
            .userScore(userScoreFromInput())
            .lastModified(game.getLastModified())
            .genres(genresFromInput())
            .libraries(game.getLibraries())
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
        return game.getGenres();
    }

    private Opt<String> readText(TextInputControl control) {
        return Opt.ofNullable(Strings.emptyToNull(control.getText())).filter(text -> !isUnavailable(text));
    }

    private void doShow() {
        final FadeTransition fade = new FadeTransition(FADE_DURATION, root);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();

        stageManager.runWithBlur(stage::showAndWait);
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

    @FXML
    private void changeThumbnail() {

    }

    @FXML
    private void changePoster() {

    }

    @FXML
    private void reSearch() {

    }

    @FXML
    private void refresh() {

    }

    @FXML
    private void delete() {
        actionService.deleteGame(game);
    }
}
