package com.github.ykrasik.gamedex.persistence.dao.exclude;

import com.github.ykrasik.gamedex.persistence.entity.ExcludedPathEntity;
import com.gs.collections.api.list.ImmutableList;
import com.j256.ormlite.dao.Dao;

import java.nio.file.Path;
import java.sql.SQLException;

/**
 * @author Yevgeny Krasik
 */
public interface ExcludePathDao extends Dao<ExcludedPathEntity, Integer> {
    ImmutableList<ExcludedPathEntity> getAll() throws SQLException;

    ExcludedPathEntity queryByPath(Path path) throws SQLException;
}
