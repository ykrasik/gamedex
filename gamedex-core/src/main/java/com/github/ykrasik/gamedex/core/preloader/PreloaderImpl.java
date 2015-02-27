package com.github.ykrasik.gamedex.core.preloader;

import com.github.ykrasik.gamedex.common.preloader.Preloader;
import com.github.ykrasik.gamedex.core.javafx.JavaFxUtils;
import com.github.ykrasik.gamedex.core.ui.UIResources;
import javafx.animation.FadeTransition;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import lombok.Lombok;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Consumer;

/**
 * @author Yevgeny Krasik
 */
// TODO: Needs refactoring... fxml?
@Slf4j
public class PreloaderImpl implements Preloader {
    private final Image logo;
    private final Pane splashLayout;

    private final StringProperty messageProperty;
    private final ProgressBar progressBar;

    private final int max;
    private int current;

    public PreloaderImpl(int max) {
        this.max = max;
        logo = UIResources.getLogo();

        progressBar = new ProgressBar();
        progressBar.setPrefWidth(logo.getWidth());
        progressBar.setProgress(0);
        final Label progressText = new Label();

        splashLayout = new VBox();
        splashLayout.getChildren().addAll(new ImageView(logo), progressBar, progressText);
        progressText.setAlignment(Pos.CENTER);
        splashLayout.setStyle("-fx-padding: 5; -fx-background-color: cornsilk; -fx-border-width:5; -fx-border-color: linear-gradient(to bottom, chocolate, derive(chocolate, 50%));");
        splashLayout.setEffect(new DropShadow());

        messageProperty = new SimpleStringProperty();
        progressText.textProperty().bind(messageProperty);
    }

    public StringProperty messageProperty() {
        return messageProperty;
    }

    @Override
    public void message(String message) {
        current++;
        final double progress = (double) current / max;
        JavaFxUtils.runLaterIfNecessary(() -> {
            messageProperty.setValue(message);
            progressBar.setProgress(progress);
        });
    }

    @Override
    public <T> void start(Task<T> task, Consumer<T> onFinished) {
        messageProperty.set("Loading GameDex...");

        final Stage initStage = new Stage(StageStyle.UNDECORATED);
        final Scene splashScene = new Scene(splashLayout);
        final Rectangle2D bounds = Screen.getPrimary().getBounds();
        initStage.setScene(splashScene);
        initStage.setX(bounds.getMinX() + bounds.getWidth() / 2 - logo.getWidth() / 2);
        initStage.setY(bounds.getMinY() + bounds.getHeight() / 3 - logo.getHeight() / 2);
        initStage.show();

        final FadeTransition fade = new FadeTransition(Duration.seconds(0.5), splashLayout);
        fade.setFromValue(1.0);
        fade.setToValue(0.0);
        fade.setOnFinished(e -> initStage.hide());

        task.setOnSucceeded(v -> {
            onFinished.accept(task.getValue());
            fade.play();
        });
        task.setOnFailed(v -> {
            log.warn("Error: ", task.getException());
            initStage.close();
            throw Lombok.sneakyThrow(task.getException());
        });

        new Thread(task, "preloader").start();
    }
}
