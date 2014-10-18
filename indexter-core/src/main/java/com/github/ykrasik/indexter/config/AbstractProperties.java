package com.github.ykrasik.indexter.config;

import com.google.common.base.Splitter;

import java.io.InputStream;
import java.util.Properties;

/**
 * @author Yevgeny Krasik
 */
public class AbstractProperties {
    protected static final Splitter COMMA_SPLITTER = Splitter.on(',').trimResults().omitEmptyStrings();
    protected static final Splitter KEY_VALUE_SPLITTER = Splitter.on(':').trimResults().omitEmptyStrings();

    protected Properties readProperties(String url) {
        try {
            final InputStream propertiesStream = getClass().getResourceAsStream(url);
            final Properties properties = new Properties();
            properties.load(propertiesStream);
            return properties;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
