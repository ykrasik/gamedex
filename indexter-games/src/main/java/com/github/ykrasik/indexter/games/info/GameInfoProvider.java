package com.github.ykrasik.indexter.games.info;

/**
 * @author Yevgeny Krasik
 */
public enum GameInfoProvider {
    METACRITIC("Metacritic"),
    GIANT_BOMB("GiantBomb");

    private final String name;

    GameInfoProvider(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
