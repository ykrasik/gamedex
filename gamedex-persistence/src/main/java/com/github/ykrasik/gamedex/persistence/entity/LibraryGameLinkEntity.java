package com.github.ykrasik.gamedex.persistence.entity;

import com.github.ykrasik.gamedex.persistence.dao.library.LibraryGameLinkDaoImpl;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author Yevgeny Krasik
 */
@Data
@Accessors(fluent = true)
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
