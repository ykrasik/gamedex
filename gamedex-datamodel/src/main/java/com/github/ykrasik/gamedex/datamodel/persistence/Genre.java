package com.github.ykrasik.gamedex.datamodel.persistence;

import lombok.NonNull;
import lombok.Value;

/**
 * @author Yevgeny Krasik
 */
@Value
public class Genre implements Comparable<Genre> {
    @NonNull private final Id<Genre> id;
    @NonNull private final String name;

    @Override
    public int compareTo(Genre o) {
        return name.compareTo(o.name);
    }

    @Override
    public String toString() {
        return name;
    }
}
