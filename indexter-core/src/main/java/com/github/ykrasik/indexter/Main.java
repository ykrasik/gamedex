package com.github.ykrasik.indexter;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * @author Yevgeny Krasik
 */
public class Main {
    public static void main(String[] args) {
        try {
            final InputStream propertiesStream = Main.class.getResourceAsStream("config.properties");
            final Properties properties = new Properties();
            properties.load(propertiesStream);
            final String applicationKey = properties.getProperty("mashape.applicationKey");

            HttpResponse<JsonNode> response = Unirest.get("https://byroredux-metacritic.p.mashape.com/type-description/game")
                .header("X-Mashape-Key", applicationKey)
//                .field("retry", 4)
                .asJson();
            System.out.println(response);

            response = Unirest.post("https://byroredux-metacritic.p.mashape.com/search/game")
                .header("X-Mashape-Key", applicationKey)
                .field("max_pages", 1)
                .field("platform", 3)
//                .field("retry", 4)
                .field("title", "stronghold")
                .asJson();
            System.out.println(response.getBody());

            response = Unirest.post("https://byroredux-metacritic.p.mashape.com/find/game")
                .header("X-Mashape-Key", applicationKey)
                .field("platform", 3)
//                .field("retry", 4)
                .field("title", "the banner saga")
                .asJson();
            System.out.println(response.getBody());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                Unirest.shutdown();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
