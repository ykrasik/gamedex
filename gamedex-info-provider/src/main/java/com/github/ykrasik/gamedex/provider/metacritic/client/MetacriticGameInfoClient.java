package com.github.ykrasik.gamedex.provider.metacritic.client;

/**
 * @author Yevgeny Krasik
 */
public interface MetacriticGameInfoClient {
    String searchGames(String name, int platformId) throws Exception;

    String fetchDetails(String detailUrl) throws Exception;

    String fetchPlatforms() throws Exception;
}
