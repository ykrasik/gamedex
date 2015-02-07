package com.github.ykrasik.indexter.games.info.metacritic.client;

import com.github.ykrasik.indexter.AbstractUnirestClient;
import com.github.ykrasik.indexter.games.info.metacritic.config.MetacriticProperties;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.request.GetRequest;
import com.mashape.unirest.request.HttpRequestWithBody;
import com.mashape.unirest.request.body.MultipartBody;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * @author Yevgeny Krasik
 */
@RequiredArgsConstructor
public class MetacriticGameInfoClientImpl extends AbstractUnirestClient implements MetacriticGameInfoClient {
    @NonNull private final MetacriticProperties properties;

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
    public String fetchDetails(String detailUrl) throws Exception {
        final GetRequest request = get("https://byroredux-metacritic.p.mashape.com/details")
            .field("url", detailUrl);
        LOG.debug("Request: {}", request.getHttpRequest().getUrl());

        final HttpResponse<String> httpResponse = request.asString();
        return assertOkAndGet(httpResponse);
    }

    @Override
    public String fetchPlatforms() throws Exception {
        final HttpResponse<String> httpResponse = get("https://byroredux-metacritic.p.mashape.com/type-description/game")
            .asString();
        return assertOkAndGet(httpResponse);
    }

    private GetRequest get(String url) {
        return Unirest.get(url).header("X-Mashape-Key", properties.getApplicationKey());
    }

    private HttpRequestWithBody post(String url) {
        return Unirest.post(url).header("X-Mashape-Key", properties.getApplicationKey());
    }
}
