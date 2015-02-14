package com.github.ykrasik.gamedex.datamodel.persistence;

import com.github.ykrasik.gamedex.datamodel.GamePlatform;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Value;

import java.nio.file.Path;

/**
 * @author Yevgeny Krasik
 */
@Value
@EqualsAndHashCode(of = "id")
public class Library implements Comparable<Library> {
    @NonNull private final Id<Library> id;
    @NonNull private final String name;
    @NonNull private final Path path;
    @NonNull private final GamePlatform platform;

    @Override
    public int compareTo(Library o) {
        return name.compareTo(o.name);
    }
}
