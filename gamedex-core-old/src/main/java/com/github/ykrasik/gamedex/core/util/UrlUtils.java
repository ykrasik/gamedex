package com.github.ykrasik.gamedex.core.util;

import com.github.ykrasik.yava.option.Opt;
import com.google.common.io.Resources;
import lombok.SneakyThrows;

import java.awt.*;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URL;

/**
 * @author Yevgeny Krasik
 */
public final class UrlUtils {
    private UrlUtils() { }

    public static Opt<byte[]> fetchOptionalUrl(Opt<String> url) throws IOException {
        return url.flatMapX(UrlUtils::fetchData);
    }

    public static Opt<byte[]> fetchData(String urlStr) throws IOException {
        final URL url = new URL(urlStr);
        try {
            return Opt.some(Resources.toByteArray(url));
        } catch (FileNotFoundException e) {
            return Opt.none();
        }
    }

    @SneakyThrows
    public static void browseToUrl(String url) {
        Desktop.getDesktop().browse(new URI(url));
    }
}
