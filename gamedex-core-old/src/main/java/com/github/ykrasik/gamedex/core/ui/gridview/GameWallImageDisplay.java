package com.github.ykrasik.gamedex.core.ui.gridview;

import com.github.ykrasik.gamedex.core.javafx.layout.ImageDisplayType;

/**
 * @author Yevgeny Krasik
 */
public enum GameWallImageDisplay {
    FIT(ImageDisplayType.FIT, "Fit Image"),
    STRETCH(ImageDisplayType.STRETCH, "Stretch Image");
//    ENLARGE(ImageDisplayType.ENLARGE, "Enlarge Image");

    private final ImageDisplayType imageDisplayType;
    private final String displayName;

    GameWallImageDisplay(ImageDisplayType imageDisplayType, String displayName) {
        this.imageDisplayType = imageDisplayType;
        this.displayName = displayName;
    }

    public ImageDisplayType getImageDisplayType() {
        return imageDisplayType;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
