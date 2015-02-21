package com.github.ykrasik.gamedex.persistence.entity;

import com.github.ykrasik.gamedex.datamodel.GamePlatform;
import com.github.ykrasik.gamedex.persistence.dao.game.GameDaoImpl;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.Date;

/**
 * @author Yevgeny Krasik
 */
@Data
@Accessors(fluent = true)
@ToString(of = {"id", "path", "name"})
@DatabaseTable(tableName = "games", daoClass = GameDaoImpl.class)
public class GameEntity {
    public static final String ID_COLUMN = "id";
    public static final String PATH_COLUMN = "path";

    @DatabaseField(columnName = ID_COLUMN, generatedId = true)
    private int id;

    @DatabaseField(columnName = PATH_COLUMN, canBeNull = false, index = true)
    private String path;

    @DatabaseField(canBeNull = false)
    private String name;

    @DatabaseField(canBeNull = false)
    private GamePlatform platform;

    @DatabaseField(width = 4000)
    private String description;

    @DatabaseField
    private Date releaseDate;

    @DatabaseField
    private Double criticScore;

    @DatabaseField
    private Double userScore;

    @DatabaseField(canBeNull = false)
    private String metacriticDetailUrl;

    @DatabaseField
    private String giantBombDetailUrl;

    @DatabaseField(dataType = DataType.DATE_LONG, version = true)
    private Date lastModified;
}
