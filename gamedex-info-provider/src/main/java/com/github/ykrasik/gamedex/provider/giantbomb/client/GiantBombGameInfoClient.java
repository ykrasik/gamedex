package com.github.ykrasik.gamedex.provider.giantbomb.client;

/**
 * @author Yevgeny Krasik
 */
public interface GiantBombGameInfoClient {
    String searchGames(String name, int platformId) throws Exception;

    String fetchDetails(String detailUrl) throws Exception;

    String fetchCriticReview(String apiDetailUrl) throws Exception;

    String fetchUserReviews(String gameId) throws Exception;
}
