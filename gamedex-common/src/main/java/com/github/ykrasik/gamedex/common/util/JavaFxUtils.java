package com.github.ykrasik.gamedex.common.util;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.stage.Stage;
import lombok.NonNull;
import lombok.SneakyThrows;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author Yevgeny Krasik
 */
public final class JavaFxUtils {
    private JavaFxUtils() { }

    public static void runLaterIfNecessary(Runnable runnable) {
        if (Platform.isFxApplicationThread()) {
            runnable.run();
        } else {
            Platform.runLater(runnable);
        }
    }

    @SneakyThrows
    public static <T> T returnLaterIfNecessary(Callable<T> callable) {
        if (Platform.isFxApplicationThread()) {
            return callable.call();
        }

        final Holder<T> holder = new Holder<>();
        Platform.runLater(() -> {
            holder.ref = ExceptionUtils.call(callable);
            holder.latch.countDown();
        });
        holder.latch.await(10, TimeUnit.SECONDS);
        return holder.ref;
    }

    private static class Holder<T> {
        private final CountDownLatch latch = new CountDownLatch(1);
        private volatile T ref;
    }

    /**
     * A hack to get around JavaFX's limitation of only allowing the FX thread to create new stages.
     */
    public static Stage createStage() {
        return returnLaterIfNecessary(Stage::new);
    }

    public static void makeDraggable(@NonNull Stage stage, @NonNull Node root) {
        final StageDragger dragger = new StageDragger();

        // Make the stage draggable by clicking anywhere.
        root.setOnMousePressed(e -> {
            dragger.xOffset = e.getSceneX();
            dragger.yOffset = e.getSceneY();
        });
        root.setOnMouseDragged(e -> {
            stage.setX(e.getScreenX() - dragger.xOffset);
            stage.setY(e.getScreenY() - dragger.yOffset);
        });
    }

    private static class StageDragger {
        private double xOffset;
        private double yOffset;
    }
}
