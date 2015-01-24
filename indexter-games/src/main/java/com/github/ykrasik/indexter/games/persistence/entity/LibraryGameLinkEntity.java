package com.github.ykrasik.indexter.games.persistence.entity;

import com.google.common.base.MoreObjects;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.Objects;

/**
 * @author Yevgeny Krasik
 */
@DatabaseTable(tableName = "library_games")
public class LibraryGameLinkEntity {
    @DatabaseField(generatedId = true)
    private int id;

    @DatabaseField(foreign = true, canBeNull = false)
    LibraryEntity library;

    @DatabaseField(foreign = true, canBeNull = false)
    GameEntity game;

    public LibraryGameLinkEntity() {
        // For ORM.
    }

    public LibraryGameLinkEntity(LibraryEntity library, GameEntity game) {
        this.library = Objects.requireNonNull(library);
        this.game = Objects.requireNonNull(game);
    }

    public int getId() {
        return id;
    }

    public LibraryEntity getLibrary() {
        return library;
    }

    public GameEntity getGame() {
        return game;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("id", id)
            .add("library", library)
            .add("game", game)
            .toString();
    }
}
