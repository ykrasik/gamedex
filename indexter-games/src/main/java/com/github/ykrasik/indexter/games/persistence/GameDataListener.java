package com.github.ykrasik.indexter.games.persistence;

/**
 * @author Yevgeny Krasik
 */
public interface GameDataListener {
    void onUpdate(GameDataService dataService);
}
