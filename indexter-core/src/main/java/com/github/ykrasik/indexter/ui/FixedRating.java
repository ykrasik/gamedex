package com.github.ykrasik.indexter.ui;

import javafx.scene.control.Skin;
import org.controlsfx.control.Rating;

/**
 * @author Yevgeny Krasik
 */
public class FixedRating extends Rating {
    public FixedRating() {
    }

    public FixedRating(int max) {
        super(max);
    }

    public FixedRating(int max, int rating) {
        super(max, rating);
    }

    @Override
    protected Skin<?> createDefaultSkin() {
        return new FixedRatingSkin(this);
    }


}
