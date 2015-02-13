package com.github.ykrasik.gamedex.common.util;

import com.github.ykrasik.opt.Opt;
import org.apache.commons.io.IOUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;

/**
 * @author Yevgeny Krasik
 */
public final class UrlUtils {
    private UrlUtils() {
    }

    public static Opt<byte[]> fetchOptionalUrl(Opt<String> url) throws IOException {
        return url.flatMapX(UrlUtils::fetchData);
    }

    public static Opt<byte[]> fetchData(String urlStr) throws IOException {
        final URL url = new URL(urlStr);
        try {
            return Opt.of(IOUtils.toByteArray(url));
        } catch (FileNotFoundException e) {
            return Opt.absent();
        }
    }
}
