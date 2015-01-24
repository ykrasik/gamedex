package com.github.ykrasik.indexter.games.datamodel;

import com.github.ykrasik.indexter.id.Id;
import com.google.common.base.MoreObjects;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * @author Yevgeny Krasik
 */
public class LocalGame {
    private final Id<LocalGame> id;
    private final Path path;
    private final LocalDateTime lastModified;
    private final Game game;

    public LocalGame(Id<LocalGame> id, Path path, LocalDateTime lastModified, Game game) {
        this.id = Objects.requireNonNull(id);
        this.path = Objects.requireNonNull(path);
        this.lastModified = Objects.requireNonNull(lastModified);
        this.game = Objects.requireNonNull(game);
    }

    public Id<LocalGame> getId() {
        return id;
    }

    public Path getPath() {
        return path;
    }

    public Game getGame() {
        return game;
    }

    public LocalDateTime getLastModified() {
        return lastModified;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final LocalGame localGame = (LocalGame) o;

        if (!id.equals(localGame.id)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("id", id)
            .add("path", path)
            .add("game", game)
            .add("lastModified", lastModified)
            .toString();
    }
}
