package com.github.ykrasik.gamedex.core.javafx;

import com.google.common.util.concurrent.SettableFuture;
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

    /**
     * A hack to get around JavaFX's limitation of only allowing the FX thread to create new stages.
     */
    @SneakyThrows
    public static <T> T returnLaterIfNecessary(Callable<T> callable) {
        if (Platform.isFxApplicationThread()) {
            return callable.call();
        }

        final Holder<T> holder = new Holder<>();
        Platform.runLater(() -> {
            try {
                holder.future.set(callable.call());
            } catch (Exception e) {
                holder.future.setException(e);
            }
            holder.latch.countDown();
        });
        holder.latch.await(10, TimeUnit.SECONDS);
        return holder.future.get();
    }

    private static class Holder<T> {
        private final CountDownLatch latch = new CountDownLatch(1);
        private final SettableFuture<T> future = SettableFuture.create();
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
