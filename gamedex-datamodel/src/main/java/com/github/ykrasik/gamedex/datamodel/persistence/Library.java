package com.github.ykrasik.gamedex.datamodel.persistence;

import com.github.ykrasik.gamedex.datamodel.GamePlatform;
import lombok.NonNull;
import lombok.Value;

import java.nio.file.Path;

/**
 * @author Yevgeny Krasik
 */
@Value
public class Library {
    @NonNull private final Id<Library> id;
    @NonNull private final String name;
    @NonNull private final Path path;
    @NonNull private final GamePlatform platform;
}
