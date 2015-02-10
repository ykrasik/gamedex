package com.github.ykrasik.gamedex.persistence.dao;

import com.github.ykrasik.gamedex.persistence.entity.GenreEntity;
import com.github.ykrasik.gamedex.persistence.exception.DataException;
import com.j256.ormlite.dao.BaseDaoImpl;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.SelectArg;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.DatabaseTableConfig;

import java.sql.SQLException;

/**
 * @author Yevgeny Krasik
 */
public class GenreDaoImpl extends BaseDaoImpl<GenreEntity, Integer> implements GenreDao {
    private final SelectArg nameArg = new SelectArg();
    private final PreparedQuery<GenreEntity> nameQuery = queryBuilder().where()
        .eq(GenreEntity.NAME_COLUMN, nameArg)
        .prepare();

    public GenreDaoImpl(Class<GenreEntity> dataClass) throws SQLException {
        super(dataClass);
    }

    public GenreDaoImpl(ConnectionSource connectionSource, Class<GenreEntity> dataClass) throws SQLException {
        super(connectionSource, dataClass);
    }

    public GenreDaoImpl(ConnectionSource connectionSource, DatabaseTableConfig<GenreEntity> tableConfig) throws SQLException {
        super(connectionSource, tableConfig);
    }

    @Override
    public GenreEntity getOrCreateByName(String name) throws SQLException {
        nameArg.setValue(name);
        GenreEntity entity = queryForFirst(nameQuery);
        if (entity != null) {
            return entity;
        }

        entity = new GenreEntity();
        entity.setName(name);
        if (create(entity) != 1) {
            throw new DataException("Error creating genre with name: %s", name);
        }
        return entity;
    }
}
