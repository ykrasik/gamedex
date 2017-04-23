/**
 * Copyright (c) 2013, ControlsFX
 * All rights reserved.

 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * * Neither the name of ControlsFX, any associated website, nor the
 * names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.

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
package com.gitlab.ykrasik.gamedex.ui.widgets

import com.sun.javafx.css.StyleManager
import com.sun.javafx.scene.control.skin.BehaviorSkinBase
import impl.org.controlsfx.behavior.RatingBehavior
import javafx.geometry.Orientation
import javafx.scene.Node
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.layout.Region
import javafx.scene.layout.VBox
import javafx.scene.shape.Rectangle
import org.controlsfx.control.Rating
import org.controlsfx.tools.Utils
import java.util.*

class FixedRatingSkin(control: Rating) : BehaviorSkinBase<Rating, RatingBehavior>(control, RatingBehavior(control)) {
//    private final EventHandler<MouseEvent> mouseMoveHandler = new EventHandler<MouseEvent>() {
//        @Override public void handle(MouseEvent event) {
//
//        	// if we support updateOnHover, calculate the intended rating based on the mouse
//        	// location and update the control property with it.
//
//            if (updateOnHover) {
//            	updateRatingFromMouseEvent(event);
//            }
//        }
//    };
//
//    private final EventHandler<MouseEvent> mouseClickHandler = new EventHandler<MouseEvent>() {
//        @Override public void handle(MouseEvent event) {
//
//        	// if we are not updating on hover, calculate the intended rating based on the mouse
//        	// location and update the control property with it.
//
//            if (! updateOnHover) {
//            	updateRatingFromMouseEvent(event);
//            }
//        }
//    };
//
//    private void updateRatingFromMouseEvent(MouseEvent event) {
//    	Rating control = getSkinnable();
//    	if (! control.ratingProperty().isBound()) {
//        	Point2D mouseLocation = new Point2D(event.getSceneX(), event.getSceneY());
//    		control.setRating(calculateRating(mouseLocation));
//    	}
//    }

//    private var updateOnHover = false
    private var partialRating = false

    // the container for the traditional rating control. If updateOnHover and
    // partialClipping are disabled, this will show a combination of strong
    // and non-strong graphics, depending on the current rating value
    private var backgroundContainer: Pane? = null

    // the container for the strong graphics which may be partially clipped.
    // Note that this only exists if updateOnHover or partialClipping is enabled.
    private var foregroundContainer: Pane? = null

    private var rating = -1.0

    private var forgroundClipRect: Rectangle? = null

    init {
//        this.updateOnHover = control.isUpdateOnHover
        this.partialRating = control.isPartialRating

        // init
        recreateButtons()
        updateRating()
        // -- end init

        registerChangeListener(control.ratingProperty(), "RATING") //$NON-NLS-1$
        registerChangeListener(control.maxProperty(), "MAX") //$NON-NLS-1$
        registerChangeListener(control.orientationProperty(), "ORIENTATION") //$NON-NLS-1$
//        registerChangeListener(control.updateOnHoverProperty(), "UPDATE_ON_HOVER") //$NON-NLS-1$
        registerChangeListener(control.partialRatingProperty(), "PARTIAL_RATING") //$NON-NLS-1$
        // added to ensure clip is correctly calculated when control is first shown:
        registerChangeListener(control.boundsInLocalProperty(), "BOUNDS") //$NON-NLS-1$
    }

    override fun handleControlPropertyChanged(p: String?) {
        super.handleControlPropertyChanged(p)

        when (p) {
            "RATING" -> updateRating()
            "MAX" -> recreateButtons()
            "ORIENTATION" -> recreateButtons()
            "PARTIAL_RATING" -> {
                this.partialRating = skinnable.isPartialRating
                recreateButtons()
            }
//            "UPDATE_ON_HOVER" -> {
//                this.updateOnHover = skinnable.isUpdateOnHover
//                recreateButtons()
//            }
            "BOUNDS" ->
                if (this.partialRating) {
                    updateClip()
                }
        }
    }

    private fun recreateButtons() {
        backgroundContainer = null
        foregroundContainer = null

        backgroundContainer = if (isVertical) VBox() else HBox()
        backgroundContainer!!.styleClass.add("container") //$NON-NLS-1$
        children.setAll(backgroundContainer)

        if (/*updateOnHover || */partialRating) {
            foregroundContainer = if (isVertical) VBox() else HBox()
            foregroundContainer!!.styleClass.add("container") //$NON-NLS-1$
            foregroundContainer!!.isMouseTransparent = true
            children.add(foregroundContainer)

            forgroundClipRect = Rectangle()
            foregroundContainer!!.clip = forgroundClipRect

        }

        for (index in 0..skinnable.max) {
            val backgroundNode = createRatingStar()

            if (index > 0) {
                if (isVertical) {
                    backgroundContainer!!.children.add(0, backgroundNode)
                } else {
                    backgroundContainer!!.children.add(backgroundNode)
                }

                if (partialRating) {
                    val foregroundNode = createRatingStar()
                    foregroundNode.styleClass.add(STRONG)
                    foregroundNode.isMouseTransparent = true

                    if (isVertical) {
                        foregroundContainer!!.children.add(0, foregroundNode)
                    } else {
                        foregroundContainer!!.children.add(foregroundNode)
                    }
                }
            }
        }

        updateRating()
    }

//    // Calculate the rating based on a mouse position (in Scene coordinates).
//    // If we support partial ratings, the value is calculated directly.
//    // Otherwise the ceil of the value is computed.
//    private double calculateRating(Point2D sceneLocation) {
//        final Point2D b = backgroundContainer.sceneToLocal(sceneLocation);
//
//        final double x = b.getX();
//        final double y = b.getY();
//
//        final Rating control = getSkinnable();
//
//        final int max = control.getMax();
//        final double w = control.getWidth() - (snappedLeftInset() + snappedRightInset());
//        final double h = control.getHeight() - (snappedTopInset() + snappedBottomInset());
//
//        double newRating = -1;
//
//        if (isVertical()) {
//            newRating = ((h - y) / h) * max;
//        } else {
//            newRating = (x / w) * max;
//        }
//
//        if (! partialRating) {
//            newRating = Utils.clamp(1, Math.ceil(newRating), control.getMax());
//        }
//
//        return newRating;
//    }

    private fun updateClip() {
        val control = skinnable
        val h = control.height - (snappedTopInset() + snappedBottomInset())
        val w = control.width - (snappedLeftInset() + snappedRightInset())

        if (isVertical) {
            val y = h * rating / control.max
            forgroundClipRect!!.relocate(0.0, h - y)
            forgroundClipRect!!.width = control.width
            forgroundClipRect!!.height = y
        } else {
            val x = w * rating / control.max
            forgroundClipRect!!.width = x
            forgroundClipRect!!.height = control.height
        }

    }

//    private double getSpacing() {
//        return (backgroundContainer instanceof HBox) ?
//                ((HBox)backgroundContainer).getSpacing() :
//                ((VBox)backgroundContainer).getSpacing();
//    }

    private fun createRatingStar(): Node {
        val btn = Region()
        btn.styleClass.add("nonHoverableStar") //$NON-NLS-1$

//        btn.setOnMouseMoved(mouseMoveHandler);
//        btn.setOnMouseClicked(mouseClickHandler);
        return btn
    }

    // Update the skin based on a new value for the rating.
    // If we support partial ratings, updates the clip.
    // Otherwise, updates the style classes for the buttons.

    private fun updateRating() {

        val newRating = skinnable.rating

        if (newRating == rating) return

        rating = Utils.clamp(0.0, newRating, skinnable.max.toDouble())

        if (partialRating) {
            updateClip()
        } else {
            updateButtonStyles()
        }
    }

    private fun updateButtonStyles() {
        val max = skinnable.max

        // make a copy of the buttons list so that we can reverse the order if
        // the list is vertical (as the buttons are ordered bottom to top).
        val buttons = ArrayList(backgroundContainer!!.children)
        if (isVertical) {
            Collections.reverse(buttons)
        }

        for (i in 0..max - 1) {
            val button = buttons[i]

            val styleClass = button.styleClass
            val containsStrong = styleClass.contains(STRONG)

            if (i < rating) {
                if (!containsStrong) {
                    styleClass.add(STRONG)
                }
            } else if (containsStrong) {
                styleClass.remove(STRONG)
            }
        }
    }

    private val isVertical: Boolean
        get() = skinnable.orientation == Orientation.VERTICAL

    override fun computeMaxWidth(height: Double, topInset: Double, rightInset: Double, bottomInset: Double, leftInset: Double): Double {
        return super.computePrefWidth(height, topInset, rightInset, bottomInset, leftInset)
    }

    companion object {

        init {
            // refer to ControlsFXControl for why this is necessary
            StyleManager.getInstance().addUserAgentStylesheet(
                FixedRatingSkin::class.java.getResource("fixed-rating.css").toExternalForm()) //$NON-NLS-1$
        }

        private val STRONG = "strong" //$NON-NLS-1$
    }
}
