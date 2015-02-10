package com.github.ykrasik.gamedex.persistence.dao;

import com.github.ykrasik.gamedex.persistence.entity.GameEntity;
import com.j256.ormlite.dao.Dao;

import java.nio.file.Path;
import java.sql.SQLException;

/**
 * @author Yevgeny Krasik
 */
public interface GameDao extends Dao<GameEntity, Integer> {
    GameEntity queryByPath(Path path) throws SQLException;
}
