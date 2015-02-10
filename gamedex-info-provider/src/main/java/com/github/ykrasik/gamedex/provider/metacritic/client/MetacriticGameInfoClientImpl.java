package com.github.ykrasik.gamedex.provider.metacritic.client;

import com.github.ykrasik.gamedex.provider.AbstractUnirestClient;
import com.github.ykrasik.gamedex.provider.metacritic.config.MetacriticProperties;
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
        final MultipartBody request = createPostRequest("https://byroredux-metacritic.p.mashape.com/search/game")
            .field("platform", platformId)
            .field("title", name);
        return send(request);
    }

    @Override
    public String fetchDetails(String detailUrl) throws Exception {
        final GetRequest request = createGetRequest("https://byroredux-metacritic.p.mashape.com/details")
            .field("url", detailUrl);
        return send(request);
    }

    @Override
    public String fetchPlatforms() throws Exception {
        final GetRequest request = createGetRequest("https://byroredux-metacritic.p.mashape.com/type-description/game");
        return send(request);
    }

    private GetRequest createGetRequest(String url) {
        return Unirest.get(url).header("X-Mashape-Key", properties.getApplicationKey());
    }

    private HttpRequestWithBody createPostRequest(String url) {
        return Unirest.post(url).header("X-Mashape-Key", properties.getApplicationKey());
    }
}
