package com.gitlab.ykrasik.gamedex.core.ui.view.widgets

import com.gitlab.ykrasik.gamedex.core.util.ImageDisplayType
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.geometry.HPos
import javafx.geometry.VPos
import javafx.scene.Node
import javafx.scene.image.ImageView
import javafx.scene.layout.Pane

/**
 * User: ykrasik
 * Date: 25/12/2016
 * Time: 17:36
 */
class ImageViewLimitedPane(private val imageView: ImageView) : Pane() {
    val imageDisplayTypeProperty = SimpleObjectProperty<ImageDisplayType>()

    init {
        super.getChildren().add(imageView)
        imageView.isSmooth = true

        minHeightProperty().bind(maxHeightProperty())
        prefHeightProperty().bind(maxHeightProperty())

        imageView.fitHeightProperty().bind(maxHeightProperty())
        imageView.fitWidthProperty().bind(maxWidthProperty())

        imageDisplayTypeProperty.addListener { observable, oldValue, newValue -> requestParentLayout() }
    }

    override fun layoutChildren() {
        layoutInArea(imageView, 0.0, 0.0, maxWidth, maxHeight, 0.0, HPos.CENTER, VPos.CENTER)
    }

    override fun resize(width: Double, height: Double) {
        super.resize(maxWidth, maxHeight)

        when (imageDisplayTypeProperty.get()) {
            ImageDisplayType.fit -> fitImage(true)
            ImageDisplayType.stretch -> stretchImage()
//            case ENLARGE: enlargeImage(); break;
            else -> null//throw IllegalArgumentException("Invalid imageDisplayType: ${imageDisplayTypeProperty.get()}")
        }
    }

    private fun fitImage(preserveRatio: Boolean) {
        imageView.isPreserveRatio = preserveRatio
    }

    private fun stretchImage() {
        val cellHeight = height
        val cellWidth = width

        val image = imageView.image
        val imageHeight = image.height
        val imageWidth = image.width

        // If the image is fit into the cell, this will be it's size.
        val heightRatio = cellHeight / imageHeight
        val widthRatio = cellWidth / imageWidth
        val fitRatio = Math.min(heightRatio, widthRatio)
        val imageFitHeight = imageHeight * fitRatio
        val imageFitWidth = imageWidth * fitRatio

        // Calculate the ratio by which we need to stretch the image to make it fill the whole cell.
        val stretchHeightRatio = cellHeight / imageFitHeight
        val stretchWidthRatio = cellWidth / imageFitWidth
        val stretchRatio = Math.max(stretchHeightRatio, stretchWidthRatio)

        // If stretchRatio isn't bigger than maxStretch, stretch the image.
        val preserveRatio = Math.abs(stretchRatio - 1) > maxStretch
        fitImage(preserveRatio)
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

    @Deprecated("")
    override fun getChildren(): ObservableList<Node> {
        return FXCollections.unmodifiableObservableList(super.getChildren())
    }

    companion object {
        // TODO: Property
        private val maxStretch = 0.5
    }
}