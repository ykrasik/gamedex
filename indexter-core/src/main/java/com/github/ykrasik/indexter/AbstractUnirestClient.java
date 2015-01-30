package com.github.ykrasik.indexter;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;

import java.net.HttpURLConnection;

/**
 * @author Yevgeny Krasik
 */
public class AbstractUnirestClient extends AbstractService {
    @Override
    protected void doStart() throws Exception {

    }

    @Override
    protected void doStop() throws Exception {
        Unirest.shutdown();
    }

    protected <T> T assertOkAndGet(HttpResponse<T> httpResponse) {
        if (httpResponse.getCode() != HttpURLConnection.HTTP_OK) {
            throw new RuntimeException("HTTP response is not OK, code: " + httpResponse.getCode());
        }
        return httpResponse.getBody();
    }
}
