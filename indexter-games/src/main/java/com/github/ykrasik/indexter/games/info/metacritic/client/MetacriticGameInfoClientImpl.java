package com.github.ykrasik.indexter.games.info.metacritic.client;

import com.github.ykrasik.indexter.AbstractUnirestClient;
import com.github.ykrasik.indexter.games.info.metacritic.config.MetacriticProperties;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.request.GetRequest;
import com.mashape.unirest.request.HttpRequestWithBody;
import com.mashape.unirest.request.body.MultipartBody;

import java.util.Objects;

/**
 * @author Yevgeny Krasik
 */
public class MetacriticGameInfoClientImpl extends AbstractUnirestClient implements MetacriticGameInfoClient {
    private final MetacriticProperties properties;

    public MetacriticGameInfoClientImpl(MetacriticProperties properties) {
        this.properties = Objects.requireNonNull(properties);
    }

    @Override
    public String fetchPlatforms() throws Exception {
        final HttpResponse<String> httpResponse = get("https://byroredux-metacritic.p.mashape.com/type-description/game")
            .asString();
        return assertOkAndGet(httpResponse);
    }

    @Override
    public String searchGames(String name, int platformId) throws Exception {
        final MultipartBody request = post("https://byroredux-metacritic.p.mashape.com/search/game")
            .field("platform", platformId)
            .field("title", name);
        LOG.debug("Request: {}", request.getHttpRequest().getUrl());

        final HttpResponse<String> httpResponse = request.asString();
        return assertOkAndGet(httpResponse);
    }

    @Override
    public String fetchDetails(String name, int platformId) throws Exception {
        final MultipartBody request = post("https://byroredux-metacritic.p.mashape.com/find/game")
            .field("platform", platformId)
            .field("title", name);
        LOG.debug("Request: {}", request.getHttpRequest().getUrl());

        final HttpResponse<String> httpResponse = request.asString();
        return assertOkAndGet(httpResponse);
    }

    private GetRequest get(String url) {
        return Unirest.get(url).header("X-Mashape-Key", properties.getApplicationKey());
    }

    private HttpRequestWithBody post(String url) {
        return Unirest.post(url).header("X-Mashape-Key", properties.getApplicationKey());
    }
}
