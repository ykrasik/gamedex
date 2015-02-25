package com.github.ykrasik.gamedex.core.config.type;

/**
 * @author Yevgeny Krasik
 */
public enum GameSort {
    NAME_ASC("Name \u2191"),
    NAME_DESC("Name \u2193"),
    CRITIC_SCORE_ASC("Critic Score \u2191"),
    CRITIC_SCORE_DESC("Critic Score \u2193"),
    USER_SCORE_ASC("User Score \u2191"),
    USER_SCORE_DESC("User Score \u2193"),
    RELEASE_DATE_ASC("Release Date \u2191"),
    RELEASE_DATE_DESC("Release Date \u2193"),
    DATE_ADDED_ASC("Date Added \u2191"),
    DATE_ADDED_DESC("Date Added \u2193");

    private final String displayName;

    GameSort(String displayName) {
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
