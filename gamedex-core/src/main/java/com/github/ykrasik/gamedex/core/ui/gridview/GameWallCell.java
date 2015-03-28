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

import com.github.ykrasik.gamedex.core.javafx.MoreBindings;
import com.github.ykrasik.gamedex.core.javafx.layout.ImageDisplayType;
import com.github.ykrasik.gamedex.core.javafx.layout.ImageViewLimitedPane;
import com.github.ykrasik.gamedex.core.service.config.ConfigService;
import com.github.ykrasik.gamedex.core.service.image.ImageService;
import com.github.ykrasik.gamedex.core.ui.UIResources;
import com.github.ykrasik.gamedex.datamodel.persistence.Game;
import com.github.ykrasik.opt.Opt;
import javafx.beans.binding.Binding;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
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
    private final ImageView imageView = new ImageView();
    private final ImageViewLimitedPane imageViewLimitedPane = new ImageViewLimitedPane(imageView);

    private final ConfigService configService;
    private final ImageService imageService;

    private Opt<Task<Image>> loadingTask = Opt.absent();

    /**
     * Create ImageGridCell instance
     */
    public GameWallCell(@NonNull ConfigService configService, @NonNull ImageService imageService) {
        this.configService = configService;
        this.imageService = imageService;

        // Really annoying, no idea why JavaFX does this, but it offsets by 1 pixel.
        imageViewLimitedPane.setTranslateX(-1);

        imageViewLimitedPane.maxHeightProperty().bind(this.heightProperty());
        imageViewLimitedPane.maxWidthProperty().bind(this.widthProperty());

        // Clip the cell's corners to be round after the cell's size is calculated.
        final Rectangle clip = new Rectangle();
        clip.setX(1);
        clip.setY(1);
        clip.setArcWidth(20);
        clip.setArcHeight(20);
        final ChangeListener<Number> clipListener = (observable, oldValue, newValue) -> {
            clip.setWidth(imageViewLimitedPane.getWidth() - 2);
            clip.setHeight(imageViewLimitedPane.getHeight() - 2);
        };
        imageViewLimitedPane.setClip(clip);
        imageViewLimitedPane.heightProperty().addListener(clipListener);
        imageViewLimitedPane.widthProperty().addListener(clipListener);

        final Binding<ImageDisplayType> binding = MoreBindings.transformBinding(configService.gameWallImageDisplayProperty(), imageDisplay -> {
            final Image image = imageView.getImage();
            if (image == UIResources.loading() || image == UIResources.notAvailable()) {
                return ImageDisplayType.FIT;
            } else {
                return imageDisplay.getImageDisplayType();
            }
        });
        imageView.imageProperty().addListener((observable, oldValue, newValue) -> {
            binding.invalidate();
        });
        imageViewLimitedPane.imageDisplayTypeProperty().bind(binding);

        getStyleClass().add("image-grid-cell"); //$NON-NLS-1$
        setText(null);
    }

    @Override
    protected void updateItem(Game item, boolean empty) {
        cancelPrevTask();
        super.updateItem(item, empty);

        if (!empty) {
            fetchImage(item);
            setGraphic(imageViewLimitedPane);
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
}