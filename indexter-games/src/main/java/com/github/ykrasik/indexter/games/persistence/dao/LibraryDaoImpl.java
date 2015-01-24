package com.github.ykrasik.indexter.games.persistence.dao;

import com.github.ykrasik.indexter.games.persistence.entity.LibraryEntity;
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
    private final SelectArg pathArg = new SelectArg();
    private final PreparedQuery<LibraryEntity> pathQuery = queryBuilder().where()
        .eq(LibraryEntity.PATH_COLUMN_NAME, pathArg)
        .prepare();

    public LibraryDaoImpl(Class<LibraryEntity> dataClass) throws SQLException {
        super(dataClass);
    }

    public LibraryDaoImpl(ConnectionSource connectionSource, Class<LibraryEntity> dataClass) throws SQLException {
        super(connectionSource, dataClass);
    }

    public LibraryDaoImpl(ConnectionSource connectionSource, DatabaseTableConfig<LibraryEntity> tableConfig) throws SQLException {
        super(connectionSource, tableConfig);
    }

    @Override
    public LibraryEntity queryByPath(Path path) throws SQLException {
        pathArg.setValue(path.toString());
        return queryForFirst(pathQuery);
    }
}
