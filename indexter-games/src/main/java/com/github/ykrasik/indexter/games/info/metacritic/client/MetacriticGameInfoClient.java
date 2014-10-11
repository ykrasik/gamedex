package com.github.ykrasik.indexter.games.info.metacritic.client;

/**
 * @author Yevgeny Krasik
 */
public interface MetacriticGameInfoClient {
    String fetchPlatforms() throws Exception;

    String searchGames(String name, int platformId) throws Exception;

    String fetchDetails(String name, int platformId) throws Exception;
}
