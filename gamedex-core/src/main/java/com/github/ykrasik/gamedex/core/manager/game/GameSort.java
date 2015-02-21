package com.github.ykrasik.gamedex.core.manager.game;

import com.github.ykrasik.gamedex.common.enums.EnumIdConverter;
import com.github.ykrasik.gamedex.common.enums.IdentifiableEnum;

/**
 * @author Yevgeny Krasik
 */
public enum GameSort implements IdentifiableEnum<String> {
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

    private static final EnumIdConverter<String, GameSort> VALUES = new EnumIdConverter<>(GameSort.class);

    private final String key;

    GameSort(String key) {
        this.key = key;
    }

    @Override
    public String getKey() {
        return key;
    }

    public static GameSort fromString(String name) {
        return VALUES.get(name);
    }

    @Override
    public String toString() {
        return key;
    }
}
