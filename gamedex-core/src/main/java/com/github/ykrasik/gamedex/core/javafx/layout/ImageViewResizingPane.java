package com.github.ykrasik.gamedex.core.javafx.layout;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.HPos;
import javafx.geometry.Orientation;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Yevgeny Krasik
 */
@Slf4j
public class ImageViewResizingPane extends Pane {
    private static final Double MAX_ENLARGE_RATIO = 2.0;    // FIXME: Property

    private final ImageView imageView;

    public ImageViewResizingPane(@NonNull ImageView imageView) {
        this.imageView = imageView;
        super.getChildren().add(imageView);
    }

    @Override
    protected void layoutChildren() {
        layoutInArea(imageView, 0, 0, imageView.getFitWidth(), imageView.getFitHeight(), 0, HPos.CENTER, VPos.CENTER);
    }

    @Override
    public void resize(double width, double height) {
        final Image image = getImage();
        final double imageWidth = image.getWidth();
        final double imageHeight = image.getHeight();
        final double normalizedWidth = Math.min(width, imageWidth * MAX_ENLARGE_RATIO);
        final double normalizedHeight = Math.min(height, imageHeight * MAX_ENLARGE_RATIO);

        imageView.setFitHeight(normalizedHeight);
        imageView.setFitWidth(normalizedWidth);
        if (Math.abs(normalizedWidth - width) <= 10 &&
            Math.abs(normalizedHeight - height) <= 10) {
            // If there are a few tiny pixel margins between the image and the border (no idea why JavaFX does this, but it does),
            // stretch the image to fill those in.
            imageView.setPreserveRatio(false);
        } else {
            imageView.setPreserveRatio(true);
        }

        super.resize(normalizedWidth, normalizedHeight);
    }

    @Override
    protected double computePrefWidth(double availableHeight) {
        if (availableHeight <= 0) {
            return availableHeight;
        }

        final double ratio = getImageRatio();
        return availableHeight / ratio;
    }

    @Override
    protected double computePrefHeight(double availableWidth) {
        if (availableWidth <= 0) {
            return availableWidth;
        }

        final double ratio = getImageRatio();
        return availableWidth * ratio;
    }

    @Override
    protected double computeMinWidth(double availableHeight) {
        final double prefWidth = computePrefWidth(availableHeight);
        return limitByMaxWidth(prefWidth);
    }

    @Override
    protected double computeMinHeight(double availableWidth) {
        final double prefHeight = computePrefHeight(availableWidth);
        return limitByMaxHeight(prefHeight);
    }

    @Override
    public Orientation getContentBias() {
        return Orientation.VERTICAL;
    }

    @Override
    @Deprecated
    public ObservableList<Node> getChildren() {
        return FXCollections.unmodifiableObservableList(super.getChildren());
    }

    private Image getImage() {
        return imageView.getImage();
    }

    private double getImageRatio() {
        final Image image = getImage();
        final double height = image.getHeight();
        final double width = image.getWidth();
        return height / width;
    }

    private double limitByMaxWidth(double width) {
        final double maxWidth = getMaxWidth();
        return limitIfApplicable(width, maxWidth);
    }

    private double limitByMaxHeight(double height) {
        final double maxHeight = getMaxHeight();
        return limitIfApplicable(height, maxHeight);
    }

    private double limitIfApplicable(double value, double max) {
        return max > 0 ? Math.min(value, max) : value;
    }
}
