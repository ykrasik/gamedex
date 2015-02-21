package com.github.ykrasik.gamedex.persistence.dao.exclude;

import com.github.ykrasik.gamedex.persistence.entity.ExcludedPathEntity;
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
public class ExcludePathDaoImpl extends BaseDaoImpl<ExcludedPathEntity, Integer> implements ExcludePathDao {
    public ExcludePathDaoImpl(Class<ExcludedPathEntity> dataClass) throws SQLException {
        super(dataClass);
    }

    public ExcludePathDaoImpl(ConnectionSource connectionSource, Class<ExcludedPathEntity> dataClass) throws SQLException {
        super(connectionSource, dataClass);
    }

    public ExcludePathDaoImpl(ConnectionSource connectionSource, DatabaseTableConfig<ExcludedPathEntity> tableConfig) throws SQLException {
        super(connectionSource, tableConfig);
    }

    private final SelectArg pathArg = new SelectArg();
    private final PreparedQuery<ExcludedPathEntity> pathQuery = queryBuilder().where()
        .eq(ExcludedPathEntity.PATH_COLUMN, pathArg)
        .prepare();

    @Override
    public ImmutableList<ExcludedPathEntity> getAll() throws SQLException {
        return Lists.immutable.ofAll(queryForAll());
    }

    @Override
    public ExcludedPathEntity queryByPath(Path path) throws SQLException {
        pathArg.setValue(path.toString());
        return queryForFirst(pathQuery);
    }
}
