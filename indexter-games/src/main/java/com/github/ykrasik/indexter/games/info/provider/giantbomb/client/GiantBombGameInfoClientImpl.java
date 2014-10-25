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
        LOG.debug("Request: {}", request.getUrl());

        final HttpResponse<String> httpResponse = request.asString();
        return assertOkAndGet(httpResponse);
    }

    @Override
    public String fetchDetails(String apiDetailUrl) throws Exception {
        final GetRequest request = get(apiDetailUrl)
            .field("field_list", "name,deck,genres,image,original_release_date,publishers,developers,site_detail_url,reviews");
        LOG.debug("Request: {}", request.getUrl());

        final HttpResponse<String> httpResponse = request.asString();
        return assertOkAndGet(httpResponse);
    }

    private GetRequest get(String url) {
        return Unirest.get(url)
            .field("api_key", properties.getApplicationKey())
            .field("format", "json");
    }
}
