package com.github.ykrasik.indexter;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.request.BaseRequest;

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

    protected String send(BaseRequest request) throws Exception {
        LOG.debug("Request: {}", request.getHttpRequest().getUrl());
        final HttpResponse<String> httpResponse = request.asString();
        if (httpResponse.getCode() != HttpURLConnection.HTTP_OK) {
            throw new RuntimeException("HTTP response is not OK, code: " + httpResponse.getCode());
        }
        final String response = httpResponse.getBody();
        LOG.debug("Response: {}", response);
        return response;
    }
}
