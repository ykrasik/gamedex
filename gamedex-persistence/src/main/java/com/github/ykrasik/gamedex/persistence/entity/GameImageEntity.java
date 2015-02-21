package com.github.ykrasik.gamedex.persistence.entity;

import com.github.ykrasik.gamedex.persistence.dao.game.GameImageDaoImpl;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * @author Yevgeny Krasik
 */
@Data
@Accessors(fluent = true)
@ToString(of = {"game", "type" })
@DatabaseTable(tableName = "game_images", daoClass = GameImageDaoImpl.class)
public class GameImageEntity {
    public static final String GAME_ID_COLUMN = "game_id";
    public static final String TYPE_COLUMN = "type";

    @DatabaseField(columnName = GAME_ID_COLUMN, foreign = true, canBeNull = false)
    private GameEntity game;

    @DatabaseField(columnName = TYPE_COLUMN, canBeNull = false)
    private GameImageEntityType type;

    @DatabaseField(dataType = DataType.BYTE_ARRAY, canBeNull = false)
    private byte[] data;
}
