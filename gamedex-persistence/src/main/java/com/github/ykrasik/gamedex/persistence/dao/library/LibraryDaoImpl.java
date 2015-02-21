package com.github.ykrasik.gamedex.persistence.dao.library;

import com.github.ykrasik.gamedex.persistence.entity.LibraryEntity;
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
public class LibraryDaoImpl extends BaseDaoImpl<LibraryEntity, Integer> implements LibraryDao {
    public LibraryDaoImpl(Class<LibraryEntity> dataClass) throws SQLException {
        super(dataClass);
    }

    public LibraryDaoImpl(ConnectionSource connectionSource, Class<LibraryEntity> dataClass) throws SQLException {
        super(connectionSource, dataClass);
    }

    public LibraryDaoImpl(ConnectionSource connectionSource, DatabaseTableConfig<LibraryEntity> tableConfig) throws SQLException {
        super(connectionSource, tableConfig);
    }

    private final SelectArg pathArg = new SelectArg();
    private final PreparedQuery<LibraryEntity> pathQuery = queryBuilder().where()
        .eq(LibraryEntity.PATH_COLUMN, pathArg)
        .prepare();

    @Override
    public ImmutableList<LibraryEntity> getAll() throws SQLException {
        return Lists.immutable.ofAll(queryForAll());
    }

    @Override
    public LibraryEntity queryByPath(Path path) throws SQLException {
        pathArg.setValue(path.toString());
        return queryForFirst(pathQuery);
    }
}
