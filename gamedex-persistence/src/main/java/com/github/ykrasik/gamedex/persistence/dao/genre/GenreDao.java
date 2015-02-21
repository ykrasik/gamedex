package com.github.ykrasik.gamedex.persistence.dao.genre;

import com.github.ykrasik.gamedex.persistence.entity.GenreEntity;
import com.gs.collections.api.list.ImmutableList;
import com.j256.ormlite.dao.Dao;

import java.sql.SQLException;

/**
 * @author Yevgeny Krasik
 */
public interface GenreDao extends Dao<GenreEntity, Integer> {
    ImmutableList<GenreEntity> getAll() throws SQLException;

    GenreEntity getOrCreateByName(String name) throws SQLException;
}
