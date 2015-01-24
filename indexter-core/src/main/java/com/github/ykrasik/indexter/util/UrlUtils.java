package com.github.ykrasik.indexter.util;

import org.apache.commons.io.IOUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.Optional;

/**
 * @author Yevgeny Krasik
 */
public final class UrlUtils {
    private UrlUtils() {
    }

    public static Optional<byte[]> fetchOptionalUrl(Optional<String> url) throws IOException {
        return Optionals.flatMap(url, UrlUtils::fetchData);
    }

    public static Optional<byte[]> fetchData(String urlStr) throws IOException {
        final URL url = new URL(urlStr);
        try {
            return Optional.of(IOUtils.toByteArray(url));
        } catch (FileNotFoundException e) {
            return Optional.empty();
        }
    }
}
