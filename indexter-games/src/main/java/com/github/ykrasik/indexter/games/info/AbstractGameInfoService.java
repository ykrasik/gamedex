package com.github.ykrasik.indexter.games.info;

import com.github.ykrasik.indexter.AbstractService;

import java.util.regex.Pattern;

/**
 * @author Yevgeny Krasik
 */
public abstract class AbstractGameInfoService extends AbstractService implements GameInfoService {
    private static final Pattern NAME_PATTERN = Pattern.compile("[\\[\\]-]");

    protected String normalizeName(String name) {
        return NAME_PATTERN.matcher(name).replaceAll("");
    }
}
