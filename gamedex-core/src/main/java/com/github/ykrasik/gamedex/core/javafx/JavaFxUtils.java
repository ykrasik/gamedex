package com.github.ykrasik.gamedex.core.javafx;

import com.google.common.util.concurrent.SettableFuture;
import javafx.application.Platform;
import javafx.beans.binding.Binding;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.stage.Stage;
import lombok.NonNull;
import lombok.SneakyThrows;

import java.util.concurrent.Callable;
import java.util.function.Function;

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

        final SettableFuture<T> future = SettableFuture.create();
        Platform.runLater(() -> {
            try {
                future.set(callable.call());
            } catch (Exception e) {
                future.setException(e);
            }
        });
        return future.get();
    }

    public static <A, B> Binding<B> transformBinding(ObservableValue<A> observable, Function<A, B> function) {
        return new ObjectBinding<B>() {
            {
                super.bind(observable);
            }

            @Override
            protected B computeValue() {
                return function.apply(observable.getValue());
            }
        };
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
