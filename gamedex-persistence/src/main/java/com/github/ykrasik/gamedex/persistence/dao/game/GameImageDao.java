package com.github.ykrasik.gamedex.persistence.dao.game;

import com.github.ykrasik.gamedex.persistence.entity.GameImageEntity;
import com.github.ykrasik.gamedex.persistence.entity.GameImageEntityType;
import com.github.ykrasik.opt.Opt;
import com.j256.ormlite.dao.Dao;

import java.sql.SQLException;

/**
 * @author Yevgeny Krasik
 */
public interface GameImageDao extends Dao<GameImageEntity, Void> {
    Opt<GameImageEntity> queryByGameId(int gameId, GameImageEntityType type) throws SQLException;

    void deleteByGameId(int gameId) throws SQLException;
}
