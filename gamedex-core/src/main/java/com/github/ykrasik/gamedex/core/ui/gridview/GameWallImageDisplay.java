package com.github.ykrasik.gamedex.core.ui.gridview;

/**
 * @author Yevgeny Krasik
 */
public enum GameWallImageDisplay {
    FIT("Fit Image"),
    STRETCH("Stretch Image"),
    ENLARGE("Enlarge Image");

    private final String displayName;

    GameWallImageDisplay(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
