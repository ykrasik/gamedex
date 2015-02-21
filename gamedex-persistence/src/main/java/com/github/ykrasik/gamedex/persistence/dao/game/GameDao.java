package com.github.ykrasik.gamedex.persistence.dao.game;

import com.github.ykrasik.gamedex.persistence.entity.GameEntity;
import com.gs.collections.api.list.ImmutableList;
import com.j256.ormlite.dao.Dao;

import java.nio.file.Path;
import java.sql.SQLException;

/**
 * @author Yevgeny Krasik
 */
public interface GameDao extends Dao<GameEntity, Integer> {
    ImmutableList<GameEntity> getAll() throws SQLException;

    GameEntity queryByPath(Path path) throws SQLException;
}
