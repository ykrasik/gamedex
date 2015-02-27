/**
 * Copyright (c) 2013, ControlsFX
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *     * Neither the name of ControlsFX, any associated website, nor the
 * names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL CONTROLSFX BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.github.ykrasik.gamedex.core.ui.gridview;

import com.github.ykrasik.gamedex.core.config.ConfigService;
import com.github.ykrasik.gamedex.core.config.ConfigType;
import com.github.ykrasik.gamedex.core.service.image.ImageService;
import com.github.ykrasik.gamedex.core.ui.UIResources;
import com.github.ykrasik.gamedex.datamodel.persistence.Game;
import com.github.ykrasik.opt.Opt;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import lombok.NonNull;
import org.controlsfx.control.GridCell;
import org.controlsfx.control.GridView;

/**
 * A {@link GridCell} that can be used to show images inside the 
 * {@link GridView} control.
 *
 * @see GridView
 */
public class GameWallCell extends GridCell<Game> {
    private static final double STRETCH_MARGIN = 1.2;
    private static final double ENLARGE_MARGIN = 0.5;
    private static final double MAX_ENLARGE = 1.5;

    private final ImageView imageView = new ImageView();
    private final StackPane innerContainer = new StackPane(imageView);
    private final StackPane outerContainer = new StackPane(innerContainer);

    private final ConfigService configService;
    private final ImageService imageService;

    private Opt<Task<Image>> loadingTask = Opt.absent();

    /**
     * Create ImageGridCell instance
     */
    public GameWallCell(@NonNull ConfigService configService, @NonNull ImageService imageService) {
        this.configService = configService;
        this.imageService = imageService;

        bindToCellSize(outerContainer);
        bindToCellSize(innerContainer);

        // I really don't know why this is needed, but without this there is a 1 pixel empty margin inside the cell.
        outerContainer.setTranslateX(-1);

        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);

        // Clip the cell's corners to be round after the cell's size is calculated.
        final ChangeListener<Number> clipListener = (observable, oldValue, newValue) -> {
            final Rectangle clip = new Rectangle(1, 1, outerContainer.getMaxWidth() - 2, outerContainer.getMaxHeight() - 2);
            clip.setArcWidth(20);
            clip.setArcHeight(20);
            outerContainer.setClip(clip);
        };
        heightProperty().addListener(clipListener);
        widthProperty().addListener(clipListener);

        imageView.imageProperty().addListener((observable, oldValue, newValue) -> handleNewImage(newValue));

        gameWallImageDisplayProperty().addListener((observable, oldValue, newValue) -> {
            // Refresh the current image when the display type changes.
            handleNewImage(imageView.getImage());
        });

        getStyleClass().add("image-grid-cell"); //$NON-NLS-1$
        setText(null);
    }

    private void bindToCellSize(StackPane stackPane) {
        final ReadOnlyDoubleProperty cellHeightProperty = heightProperty();
        final ReadOnlyDoubleProperty cellWidthProperty = widthProperty();

        stackPane.minHeightProperty().bind(cellHeightProperty);
        stackPane.minWidthProperty().bind(cellWidthProperty);
        stackPane.maxHeightProperty().bind(cellHeightProperty);
        stackPane.maxWidthProperty().bind(cellWidthProperty);
        stackPane.prefHeightProperty().bind(cellHeightProperty);
        stackPane.prefWidthProperty().bind(cellWidthProperty);
    }

    private void handleNewImage(Image image) {
        if (image == UIResources.imageLoading()) {
            fitImage();
            return;
        }

        switch (gameWallImageDisplayProperty().get()) {
            case FIT: fitImage(); break;
            case STRETCH: stretchImage(image); break;
            case ENLARGE: enlargeImage(image); break;
        }
    }

    private void fitImage() {
        imageView.setPreserveRatio(true);
        imageView.setFitHeight(innerContainer.getMaxHeight());
        imageView.setFitWidth(innerContainer.getMaxWidth());
        imageView.setTranslateX(0);
        imageView.setTranslateY(0);
        innerContainer.setTranslateY(0);
        innerContainer.setTranslateX(0);
    }

    private void stretchImage(Image image) {
        // Allow stretching the image by up to 20% if it fills up the cell.
        if (image.getHeight() * STRETCH_MARGIN >= getHeight() ||
            image.getWidth() * STRETCH_MARGIN >= getWidth()) {
            imageView.setTranslateX(0);
            imageView.setPreserveRatio(false);
            imageView.setFitHeight(getHeight() - 2);
            imageView.setFitWidth(getWidth() - 2);
        } else {
            fitImage();
        }
    }

    private void enlargeImage(Image image) {
        final double cellHeight = getHeight();
        final double cellWidth = getWidth();
        if (cellHeight == 0 || cellWidth == 0) {
            fitImage();
            return;
        }

        final double imageHeight = image.getHeight();
        final double imageWidth = image.getWidth();

        // If the image is fit into the cell, this will be it's size.
        final double heightRatio = cellHeight / imageHeight;
        final double widthRatio = cellWidth / imageWidth;
        final double fitRatio = Math.min(heightRatio, widthRatio);
        final double imageFitHeight = imageHeight * fitRatio;
        final double imageFitWidth = imageWidth * fitRatio;

        // Calculate the ratio by which we need to enlarge the image to make it fill the whole cell.
        final double enlargeHeightRatio = cellHeight / imageFitHeight;
        final double enlargeWidthRatio = cellWidth / imageFitWidth;
        final double enlargeRatio = Math.max(enlargeHeightRatio, enlargeWidthRatio);

        // If the enlarge ratio is within ENLARGE_MARGIN, and the final enlarged image isn't enlarged
        // by more then MAX_ENLARGE, enlarge the image.
        if (Math.abs(enlargeRatio - 1) > ENLARGE_MARGIN || enlargeRatio * fitRatio > MAX_ENLARGE) {
            fitImage();
            return;
        }

        final double enlargedImageHeight = imageFitHeight * enlargeRatio;
        final double enlargedImageWidth = imageFitWidth * enlargeRatio;

        imageView.setFitHeight(enlargedImageHeight);
        imageView.setFitWidth(enlargedImageWidth);

        // Center the enlarged image.
        final double translateX = Math.abs(cellWidth - enlargedImageWidth) / 2;
        imageView.setTranslateX(-translateX);
        innerContainer.setTranslateX(translateX);

        final double translateY = Math.abs(cellHeight - enlargedImageHeight) / 2;
        imageView.setTranslateY(-translateY);
        innerContainer.setTranslateY(translateY);
    }

    @Override
    protected void updateItem(Game item, boolean empty) {
        super.updateItem(item, empty);

        if (!empty) {
            fetchImage(item);
            setGraphic(outerContainer);
        } else {
            setGraphic(null);
        }
    }

    private void fetchImage(Game game) {
        loadingTask = Opt.of(imageService.fetchThumbnail(game.getId(), imageView));
    }

    private void cancelPrevTask() {
        if (loadingTask.isPresent()) {
            final Task<Image> task = loadingTask.get();
            if (task.getState() != Worker.State.SUCCEEDED &&
                task.getState() != Worker.State.FAILED) {
                task.cancel();
            }
        }
        loadingTask = Opt.absent();
    }

    private ObjectProperty<GameWallImageDisplay> gameWallImageDisplayProperty() {
        return configService.property(ConfigType.GAME_WALL_IMAGE_DISPLAY);
    }
}