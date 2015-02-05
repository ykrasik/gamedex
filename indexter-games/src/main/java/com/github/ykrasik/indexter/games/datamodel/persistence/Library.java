package com.github.ykrasik.indexter.games.datamodel.persistence;

import com.github.ykrasik.indexter.games.datamodel.GamePlatform;
import com.github.ykrasik.indexter.id.Id;
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
