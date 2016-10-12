package com.github.ykrasik.gamedex.core.javafx.layout;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Yevgeny Krasik
 */
@Accessors(fluent = true)
@Slf4j
public class ImageViewLimitedPane extends Pane {
    // TODO: Property
    private static final double MAX_STRETCH = 0.5;

//    private static final double ENLARGE_MARGIN = 0.5;
//    private static final double MAX_ENLARGE = 1.5;

    private final ImageView imageView;

    @Getter private final ObjectProperty<ImageDisplayType> imageDisplayTypeProperty = new SimpleObjectProperty<>();

    public ImageViewLimitedPane(@NonNull ImageView imageView) {
        this.imageView = imageView;
        super.getChildren().add(imageView);
        imageView.setSmooth(true);

        minHeightProperty().bind(maxHeightProperty());
        prefHeightProperty().bind(maxHeightProperty());

        imageView.fitHeightProperty().bind(maxHeightProperty());
        imageView.fitWidthProperty().bind(maxWidthProperty());

        imageDisplayTypeProperty.addListener((observable, oldValue, newValue) -> {
            requestParentLayout();
        });
    }

    @Override
    protected void layoutChildren() {
        layoutInArea(imageView, 0, 0, getMaxWidth(), getMaxHeight(), 0, HPos.CENTER, VPos.CENTER);
    }

    @Override
    public void resize(double width, double height) {
        super.resize(getMaxWidth(), getMaxHeight());

        switch (imageDisplayTypeProperty.get()) {
            case FIT: fitImage(true); break;
            case STRETCH: stretchImage(); break;
//            case ENLARGE: enlargeImage(); break;
            default: throw new IllegalArgumentException("Invalid imageDisplayType: " + imageDisplayTypeProperty().get());
        }
    }

    private void fitImage(boolean preserveRatio) {
        imageView.setPreserveRatio(preserveRatio);
    }

    private void stretchImage() {
        final double cellHeight = getHeight();
        final double cellWidth = getWidth();

        final Image image = getImage();
        final double imageHeight = image.getHeight();
        final double imageWidth = image.getWidth();

        // If the image is fit into the cell, this will be it's size.
        final double heightRatio = cellHeight / imageHeight;
        final double widthRatio = cellWidth / imageWidth;
        final double fitRatio = Math.min(heightRatio, widthRatio);
        final double imageFitHeight = imageHeight * fitRatio;
        final double imageFitWidth = imageWidth * fitRatio;

        // Calculate the ratio by which we need to stretch the image to make it fill the whole cell.
        final double stretchHeightRatio = cellHeight / imageFitHeight;
        final double stretchWidthRatio = cellWidth / imageFitWidth;
        final double stretchRatio = Math.max(stretchHeightRatio, stretchWidthRatio);

        // If stretchRatio isn't bigger then MAX_STRETCH, stretch the image.
        final boolean preserveRatio = Math.abs(stretchRatio - 1) > MAX_STRETCH;
        fitImage(preserveRatio);
    }

//    private void enlargeImage() {
//        final double cellHeight = getHeight();
//        final double cellWidth = getWidth();
//
//        final Image image = getImage();
//        final double imageHeight = image.getHeight();
//        final double imageWidth = image.getWidth();
//
//        // If the image is fit into the cell, this will be it's size.
//        final double heightRatio = cellHeight / imageHeight;
//        final double widthRatio = cellWidth / imageWidth;
//        final double fitRatio = Math.min(heightRatio, widthRatio);
//        final double imageFitHeight = imageHeight * fitRatio;
//        final double imageFitWidth = imageWidth * fitRatio;
//
//        // Calculate the ratio by which we need to enlarge the image to make it fill the whole cell.
//        final double enlargeHeightRatio = cellHeight / imageFitHeight;
//        final double enlargeWidthRatio = cellWidth / imageFitWidth;
//        final double enlargeRatio = Math.max(enlargeHeightRatio, enlargeWidthRatio);
//
//        // If enlargeRatio is within ENLARGE_MARGIN, and the final enlarged image isn't enlarged
//        // by more then MAX_ENLARGE, enlarge the image.
//        if (Math.abs(enlargeRatio - 1) > ENLARGE_MARGIN || enlargeRatio * fitRatio > MAX_ENLARGE) {
//            fitImage(true);
//            return;
//        }
//
//        final double enlargedImageHeight = imageFitHeight * enlargeRatio;
//        final double enlargedImageWidth = imageFitWidth * enlargeRatio;
//
//        imageView.setFitHeight(enlargedImageHeight);
//        imageView.setFitWidth(enlargedImageWidth);
//
//        // Center the enlarged image.
//        final double translateX = Math.abs(cellWidth - enlargedImageWidth) / 2;
//        imageView.setTranslateX(-translateX);
//        innerContainer.setTranslateX(translateX);
//
//        final double translateY = Math.abs(cellHeight - enlargedImageHeight) / 2;
//        imageView.setTranslateY(-translateY);
//        innerContainer.setTranslateY(translateY);
//    }

    @Override
    @Deprecated
    public ObservableList<Node> getChildren() {
        return FXCollections.unmodifiableObservableList(super.getChildren());
    }

    private Image getImage() {
        return imageView.getImage();
    }
}
