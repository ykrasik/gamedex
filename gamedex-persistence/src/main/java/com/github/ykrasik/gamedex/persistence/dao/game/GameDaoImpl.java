package com.github.ykrasik.gamedex.persistence.dao.game;

import com.github.ykrasik.gamedex.persistence.entity.GameEntity;
import com.gs.collections.api.list.ImmutableList;
import com.gs.collections.impl.factory.Lists;
import com.j256.ormlite.dao.BaseDaoImpl;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.SelectArg;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.DatabaseTableConfig;

import java.nio.file.Path;
import java.sql.SQLException;

/**
 * @author Yevgeny Krasik
 */
public class GameDaoImpl extends BaseDaoImpl<GameEntity, Integer> implements GameDao {
    public GameDaoImpl(Class<GameEntity> dataClass) throws SQLException {
        super(dataClass);
    }

    public GameDaoImpl(ConnectionSource connectionSource, Class<GameEntity> dataClass) throws SQLException {
        super(connectionSource, dataClass);
    }

    public GameDaoImpl(ConnectionSource connectionSource, DatabaseTableConfig<GameEntity> tableConfig) throws SQLException {
        super(connectionSource, tableConfig);
    }

    private final SelectArg pathArg = new SelectArg();
    private final PreparedQuery<GameEntity> pathQuery = queryBuilder().where()
        .eq(GameEntity.PATH_COLUMN, pathArg)
        .prepare();

    @Override
    public ImmutableList<GameEntity> getAll() throws SQLException {
        return Lists.immutable.ofAll(queryForAll());
    }

    @Override
    public GameEntity queryByPath(Path path) throws SQLException {
        pathArg.setValue(path.toString());
        return queryForFirst(pathQuery);
    }
}
