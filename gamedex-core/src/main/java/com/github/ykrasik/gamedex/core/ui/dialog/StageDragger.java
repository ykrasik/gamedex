package com.github.ykrasik.gamedex.core.ui.dialog;

import javafx.scene.Node;
import javafx.stage.Stage;
import lombok.NonNull;

/**
 * @author Yevgeny Krasik
 */
public class StageDragger {
    private double xOffset;
    private double yOffset;

    private StageDragger(@NonNull Stage stage, @NonNull Node root) {
        // Make the stage draggable by clicking anywhere.
        root.setOnMousePressed(e -> {
            xOffset = e.getSceneX();
            yOffset = e.getSceneY();
        });
        root.setOnMouseDragged(e -> {
            stage.setX(e.getScreenX() - xOffset);
            stage.setY(e.getScreenY() - yOffset);
        });
    }

    public static StageDragger create(Stage stage, Node root) {
        return new StageDragger(stage, root);
    }
}
