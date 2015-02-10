package com.github.ykrasik.gamedex.provider;

/**
 * @author Yevgeny Krasik
 */
public enum GameInfoProviderType {
    METACRITIC("Metacritic"),
    GIANT_BOMB("GiantBomb");

    private final String name;

    GameInfoProviderType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
