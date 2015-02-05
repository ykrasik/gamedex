package com.github.ykrasik.indexter.games.persistence.entity;

import com.github.ykrasik.indexter.games.persistence.dao.GenreDaoImpl;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import lombok.Data;

/**
 * @author Yevgeny Krasik
 */
@Data
@DatabaseTable(tableName = "genres", daoClass = GenreDaoImpl.class)
public class GenreEntity {
    public static final String ID_COLUMN = "id";
    public static final String NAME_COLUMN = "name";

    @DatabaseField(columnName = ID_COLUMN, generatedId = true)
    private int id;

    @DatabaseField(columnName = NAME_COLUMN, canBeNull = false)
    private String name;
}
