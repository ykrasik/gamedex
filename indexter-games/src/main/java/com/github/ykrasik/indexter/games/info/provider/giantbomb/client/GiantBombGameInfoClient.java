package com.github.ykrasik.indexter.games.info.provider.giantbomb.client;

/**
 * @author Yevgeny Krasik
 */
public interface GiantBombGameInfoClient {
    String searchGames(String name, int platformId) throws Exception;

    String fetchDetails(String apiDetailUrl) throws Exception;

    String fetchCriticReview(String apiDetailUrl) throws Exception;

    String fetchUserReviews(String gameId) throws Exception;
}
