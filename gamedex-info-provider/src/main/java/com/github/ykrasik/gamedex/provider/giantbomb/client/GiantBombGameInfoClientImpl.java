package com.github.ykrasik.gamedex.provider.giantbomb.client;

import com.github.ykrasik.gamedex.common.util.StringUtils;
import com.github.ykrasik.gamedex.provider.AbstractUnirestClient;
import com.github.ykrasik.gamedex.provider.giantbomb.config.GiantBombProperties;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.request.GetRequest;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

import static com.github.ykrasik.gamedex.provider.giantbomb.GiantBombApi.*;

/**
 * @author Yevgeny Krasik
 */
@RequiredArgsConstructor
public class GiantBombGameInfoClientImpl extends AbstractUnirestClient implements GiantBombGameInfoClient {
    private static final String SEARCH_FIELDS = StringUtils.toCsv(Arrays.asList(DETAIL_URL, NAME, RELEASE_DATE, IMAGE));
    private static final String FETCH_DETAILS_FIELDS = StringUtils.toCsv(Arrays.asList(NAME, DESCRIPTION, RELEASE_DATE, IMAGE, GENRES));

    @NonNull private final GiantBombProperties properties;

    @Override
    public String searchGames(String name, int platformId) throws Exception {
        final GetRequest request = createGetRequest("http://www.giantbomb.com/api/games")
            .field("filter", String.format("name:%s,platforms:%d", name, platformId))
            .field("field_list", SEARCH_FIELDS);
        return send(request);
    }

    @Override
    public String fetchDetails(String detailUrl) throws Exception {
        final GetRequest request = createGetRequest(detailUrl)
            .field("field_list", FETCH_DETAILS_FIELDS);
        return send(request);
    }

    @Override
    public String fetchCriticReview(String apiDetailUrl) throws Exception {
        final GetRequest request = createGetRequest(apiDetailUrl)
            .field("field_list", "deck,publish_date,reviewer,score,site_detail_url");
        return send(request);
    }

    @Override
    public String fetchUserReviews(String gameId) throws Exception {
        final GetRequest request = createGetRequest("http://www.giantbomb.com/api/user_reviews")
            .field("game", gameId)
            .field("field_list", "deck,date_added,reviewer,score,site_detail_url")
            .field("sort", "date_added:desc");
        return send(request);
    }

    private GetRequest createGetRequest(String url) {
        return Unirest.get(url)
            .field("api_key", properties.getApplicationKey())
            .field("format", "json");
    }
}
