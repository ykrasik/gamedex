package com.github.ykrasik.gamedex.core.game;

import com.github.ykrasik.gamedex.common.enums.EnumIdConverter;
import com.github.ykrasik.gamedex.common.enums.IdentifiableEnum;
import com.github.ykrasik.gamedex.common.util.ListUtils;

import java.util.Arrays;
import java.util.List;

/**
 * @author Yevgeny Krasik
 */
public enum GameSort implements IdentifiableEnum<String> {
    NAME("Name"),
    CRITIC_SCORE("Critic Score"),
    USER_SCORE("User Score"),
    RELEASE_DATE("Release Date"),
    DATE_ADDED("Date Added");

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

    public static List<String> getKeys() {
        return ListUtils.map(Arrays.asList(values()), GameSort::getKey);
    }
}
