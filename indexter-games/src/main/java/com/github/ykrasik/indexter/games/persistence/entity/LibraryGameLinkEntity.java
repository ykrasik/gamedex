package com.github.ykrasik.indexter.games.persistence.entity;

import com.github.ykrasik.indexter.games.persistence.dao.LibraryGameLinkDaoImpl;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import lombok.Data;

/**
 * @author Yevgeny Krasik
 */
@Data
@DatabaseTable(tableName = "library_games", daoClass = LibraryGameLinkDaoImpl.class)
public class LibraryGameLinkEntity {
    public static final String LIBRARY_COLUMN = "library_id";
    public static final String GAME_COLUMN = "game_id";

    @DatabaseField(generatedId = true)
    private int id;

    @DatabaseField(columnName = LIBRARY_COLUMN, foreign = true, canBeNull = false, index = true)
    private LibraryEntity library;

    @DatabaseField(columnName = GAME_COLUMN, foreign = true, canBeNull = false, index = true)
    private GameEntity game;
}
