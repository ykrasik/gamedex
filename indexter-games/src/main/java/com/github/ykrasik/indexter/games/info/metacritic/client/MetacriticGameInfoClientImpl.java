package com.github.ykrasik.indexter.games.info.metacritic.client;

import com.github.ykrasik.indexter.AbstractService;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.Properties;

/**
 * @author Yevgeny Krasik
 */
public class MetacriticGameInfoClientImpl extends AbstractService implements MetacriticGameInfoClient {
    private final String applicationKey;

    public MetacriticGameInfoClientImpl() {
        this.applicationKey = getApplicationKey();
    }

    // FIXME: ApplicationKey should probably be injected.
    private String getApplicationKey() {
        try {
            final InputStream propertiesStream = getClass().getResourceAsStream("config.properties");
            final Properties properties = new Properties();
            properties.load(propertiesStream);
            return properties.getProperty("mashape.applicationKey");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void doStart() throws Exception {

    }

    @Override
    protected void doStop() throws Exception {
        Unirest.shutdown();
    }

    @Override
    public String fetchPlatforms() throws Exception {
        final HttpResponse<String> httpResponse = Unirest.get("https://byroredux-metacritic.p.mashape.com/type-description/game")
            .header("X-Mashape-Key", applicationKey)
            .asString();
        assertResponseOk(httpResponse);
        return httpResponse.getBody();
    }

    @Override
    public String searchGames(String name, int platformId) throws Exception {
        final HttpResponse<String> httpResponse = Unirest.post("https://byroredux-metacritic.p.mashape.com/search/game")
            .header("X-Mashape-Key", applicationKey)
            .field("platform", platformId)
            .field("title", name)
            .asString();
        assertResponseOk(httpResponse);
        return httpResponse.getBody();
    }

    @Override
    public String fetchDetails(String name, int platformId) throws Exception {
        final HttpResponse<String> httpResponse = Unirest.post("https://byroredux-metacritic.p.mashape.com/find/game")
            .header("X-Mashape-Key", applicationKey)
            .field("platform", platformId)
            .field("title", name)
            .asString();
        assertResponseOk(httpResponse);
        return httpResponse.getBody();
    }

    private <T> void assertResponseOk(HttpResponse<T> httpResponse) {
        if (httpResponse.getCode() != HttpURLConnection.HTTP_OK) {
            throw new RuntimeException("Error: " + httpResponse.getCode());
        }
    }
}
