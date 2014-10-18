package com.github.ykrasik.indexter.games.info.giantbomb.client;

/**
 * @author Yevgeny Krasik
 */
public interface GiantBombGameInfoClient {
    String searchGames(String name, int platformId) throws Exception;

    String fetchDetails(String apiDetailUrl) throws Exception;
}
