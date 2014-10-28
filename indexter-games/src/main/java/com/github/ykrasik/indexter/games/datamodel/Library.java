package com.github.ykrasik.indexter.games.datamodel;

import com.google.common.base.MoreObjects;

import java.nio.file.Path;
import java.util.Objects;

/**
 * @author Yevgeny Krasik
 */
public class Library {
    private final String name;
    private final Path path;
    private final GamePlatform platform;

    public Library(String name, Path path, GamePlatform platform) {
        this.name = Objects.requireNonNull(name);
        this.path = Objects.requireNonNull(path);
        this.platform = Objects.requireNonNull(platform);
    }

    public String getName() {
        return name;
    }

    public Path getPath() {
        return path;
    }

    public GamePlatform getPlatform() {
        return platform;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("name", name)
            .add("path", path)
            .add("platform", platform)
            .toString();
    }
}
