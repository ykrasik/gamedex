package com.github.ykrasik.indexter.games.datamodel;

import com.google.common.base.MoreObjects;

import java.nio.file.Path;
import java.util.Objects;

/**
 * @author Yevgeny Krasik
 */
public class LocalGameInfo {
    private final int id;
    private final Path path;
    private final GameInfo gameInfo;

    public LocalGameInfo(int id, Path path, GameInfo gameInfo) {
        this.id = id;
        this.path = Objects.requireNonNull(path);
        this.gameInfo = Objects.requireNonNull(gameInfo);
    }

    public int getId() {
        return id;
    }

    public Path getPath() {
        return path;
    }

    public GameInfo getGameInfo() {
        return gameInfo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final LocalGameInfo that = (LocalGameInfo) o;

        if (id != that.id) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("id", id)
            .add("path", path)
            .add("gameInfo", gameInfo)
            .toString();
    }
}
