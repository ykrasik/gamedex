package com.github.ykrasik.indexter.util;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.net.URL;

/**
 * @author Yevgeny Krasik
 */
public final class UrlUtils {
    private UrlUtils() {
    }

    public static byte[] fetchData(String urlStr) throws IOException {
        final URL url = new URL(urlStr);
        return IOUtils.toByteArray(url);
    }
}
