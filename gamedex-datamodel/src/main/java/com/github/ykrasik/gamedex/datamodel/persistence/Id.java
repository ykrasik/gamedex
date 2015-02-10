package com.github.ykrasik.gamedex.datamodel.persistence;

import lombok.Value;

/**
 * @author Yevgeny Krasik
 */
@Value
public class Id<T> {
    private final int id;
}
