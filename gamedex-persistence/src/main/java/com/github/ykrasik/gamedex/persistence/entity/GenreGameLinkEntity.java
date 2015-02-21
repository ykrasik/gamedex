package com.github.ykrasik.gamedex.persistence.entity;

import com.github.ykrasik.gamedex.persistence.dao.genre.GenreGameLinkDaoImpl;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author Yevgeny Krasik
 */
@Data
@Accessors(fluent = true)
@DatabaseTable(tableName = "genre_games", daoClass = GenreGameLinkDaoImpl.class)
public class GenreGameLinkEntity {
    public static final String GENRE_COLUMN = "genre_id";
    public static final String GAME_COLUMN = "game_id";

    @DatabaseField(generatedId = true)
    private int id;

    @DatabaseField(columnName = GENRE_COLUMN, foreign = true, canBeNull = false, index = true)
    private GenreEntity genre;

    @DatabaseField(columnName = GAME_COLUMN, foreign = true, canBeNull = false, index = true)
    private GameEntity game;
}
