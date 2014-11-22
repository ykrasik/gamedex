package com.github.ykrasik.indexter.games.info.provider.giantbomb.client;

import com.github.ykrasik.indexter.AbstractUnirestClient;
import com.github.ykrasik.indexter.games.info.provider.giantbomb.config.GiantBombProperties;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.request.GetRequest;

import java.util.Objects;

/**
 * @author Yevgeny Krasik
 */
public class GiantBombGameInfoClientImpl extends AbstractUnirestClient implements GiantBombGameInfoClient {
    private final GiantBombProperties properties;

    public GiantBombGameInfoClientImpl(GiantBombProperties properties) {
        this.properties = Objects.requireNonNull(properties);
    }

    @Override
    public String searchGames(String name, int platformId) throws Exception {
        final GetRequest request = get("http://www.giantbomb.com/api/games")
            .field("filter", String.format("name:%s,platforms:%d", name, platformId))
            .field("field_list", "name,api_detail_url,image,original_release_date")
            .field("sort", "original_release_date:desc");
        return get(request);
    }

    @Override
    public String fetchDetails(String apiDetailUrl) throws Exception {
        final GetRequest request = get(apiDetailUrl)
            .field("field_list", "name,deck,genres,image,original_release_date");
        return get(request);
    }

    @Override
    public String fetchCriticReview(String apiDetailUrl) throws Exception {
        final GetRequest request = get(apiDetailUrl)
            .field("field_list", "deck,publish_date,reviewer,score,site_detail_url");
        return get(request);
    }

    @Override
    public String fetchUserReviews(String gameId) throws Exception {
        final GetRequest request = get("http://www.giantbomb.com/api/user_reviews")
            .field("game", gameId)
            .field("field_list", "deck,date_added,reviewer,score,site_detail_url")
            .field("sort", "date_added:desc");
        return get(request);
    }

    private GetRequest get(String url) {
        return Unirest.get(url)
            .field("api_key", properties.getApplicationKey())
            .field("format", "json");
    }

    private String get(GetRequest request) throws Exception {
        LOG.debug("Request: {}", request.getUrl());
        final HttpResponse<String> httpResponse = request.asString();
        return assertOkAndGet(httpResponse);
    }
}
