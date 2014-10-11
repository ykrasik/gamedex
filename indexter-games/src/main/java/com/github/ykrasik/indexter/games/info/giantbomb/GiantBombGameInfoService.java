package com.github.ykrasik.indexter.games.info.giantbomb;

import com.cedarsoftware.util.io.JsonWriter;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import org.boon.IO;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.PropertyNamingStrategy;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.Properties;

/**
 * @author Yevgeny Krasik
 */
public class GiantBombGameInfoService {
    private final String applicationKey;
    private final ObjectMapper mapper;

    public GiantBombGameInfoService() {
        this.applicationKey = getApplicationKey();
        this.mapper = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES);
    }

    // FIXME: ApplicationKey should probably be injected.
    private String getApplicationKey() {
        try {
            final InputStream propertiesStream = getClass().getResourceAsStream("config.properties");
            final Properties properties = new Properties();
            properties.load(propertiesStream);
            return properties.getProperty("giantbomb.applicationKey");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String search(String name) throws Exception {
        final HttpResponse<InputStream> httpResponse = Unirest.get("http://www.giantbomb.com/api/search")
            .field("api_key", applicationKey)
            .field("format", "json")
            .field("resources", "game")
            .field("query", name)
            .field("field_list", "id,name,api_detail_url,image,original_release_date,deck,site_detail_url")
            .asBinary();
        if (httpResponse.getCode() != HttpURLConnection.HTTP_OK) {
            throw new RuntimeException("Error: " + httpResponse.getCode());
        }

        final String read = IO.read(httpResponse.getBody());
        return JsonWriter.formatJson(read);
    }
}
