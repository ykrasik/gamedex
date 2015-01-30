package com.github.ykrasik.indexter.games;

import com.github.ykrasik.indexter.exception.ConsumerWithException;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
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

/**
 * @author Yevgeny Krasik
 */
public class IndexterPreloader {
    private static final int SPLASH_WIDTH = 676;
    private static final int SPLASH_HEIGHT = 227;

    private final Pane splashLayout;
    private final ProgressBar progressBar;
    private final Label progressText;

    private final StringProperty message;

    public IndexterPreloader() {
        final ImageView splash = new ImageView(new Image("http://fxexperience.com/wp-content/uploads/2010/06/logo.png"));
        progressBar = new ProgressBar();
        progressBar.setPrefWidth(SPLASH_WIDTH - 20);
        progressText = new Label("Loading inDexter...");
        splashLayout = new VBox();
        splashLayout.getChildren().addAll(splash, progressBar, progressText);
        progressText.setAlignment(Pos.CENTER);
        splashLayout.setStyle("-fx-padding: 5; -fx-background-color: cornsilk; -fx-border-width:5; -fx-border-color: linear-gradient(to bottom, chocolate, derive(chocolate, 50%));");
        splashLayout.setEffect(new DropShadow());

        message = new SimpleStringProperty();
        progressText.textProperty().bind(message);
    }

    public StringProperty messageProperty() {
        return message;
    }

    public void setMessage(String message) {
        if (Platform.isFxApplicationThread()) {
            this.message.setValue(message);
        } else {
            Platform.runLater(() -> this.message.setValue(message));
        }
    }

    public <T> void start(Task<T> task, ConsumerWithException<T> consumer) {
        progressBar.setProgress(-1);
        setMessage("Loading inDexter...");

        final Stage initStage = new Stage(StageStyle.UNDECORATED);
        final Scene splashScene = new Scene(splashLayout);
        final Rectangle2D bounds = Screen.getPrimary().getBounds();
        initStage.setScene(splashScene);
        initStage.setX(bounds.getMinX() + bounds.getWidth() / 2 - SPLASH_WIDTH / 2);
        initStage.setY(bounds.getMinY() + bounds.getHeight() / 2 - SPLASH_HEIGHT / 2);
        initStage.show();

        final FadeTransition fadeSplash = new FadeTransition(Duration.seconds(0.5), splashLayout);
        fadeSplash.setFromValue(1.0);
        fadeSplash.setToValue(0.0);
        fadeSplash.setOnFinished(e -> initStage.hide());

        task.setOnSucceeded(v -> {
            fadeSplash.play();
            try {
                final T t = task.get();
                consumer.accept(t);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        task.setOnFailed(v -> {
            initStage.close();
            throw new RuntimeException(task.getException());
        });

        new Thread(task).start();
    }
}