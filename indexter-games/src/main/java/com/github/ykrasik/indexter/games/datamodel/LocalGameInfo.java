package com.github.ykrasik.indexter.games.datamodel;

import com.google.common.base.MoreObjects;

import java.nio.file.Path;
import java.util.Objects;

/**
 * @author Yevgeny Krasik
 */
public class LocalGameInfo {
    private final Path path;
    private final GameInfo gameInfo;

    public LocalGameInfo(Path path, GameInfo gameInfo) {
        this.path = Objects.requireNonNull(path);
        this.gameInfo = Objects.requireNonNull(gameInfo);
    }

    public Path getPath() {
        return path;
    }

    public GameInfo getGameInfo() {
        return gameInfo;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("path", path)
            .add("gameInfo", gameInfo)
            .toString();
    }
}
