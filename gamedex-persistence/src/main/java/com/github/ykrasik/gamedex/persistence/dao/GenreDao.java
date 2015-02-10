package com.github.ykrasik.gamedex.persistence.dao;

import com.github.ykrasik.gamedex.persistence.entity.GenreEntity;
import com.j256.ormlite.dao.Dao;

import java.sql.SQLException;

/**
 * @author Yevgeny Krasik
 */
public interface GenreDao extends Dao<GenreEntity, Integer> {
    GenreEntity getOrCreateByName(String name) throws SQLException;
}
