package com.github.ykrasik.indexter.games.persistence.dao;

import com.github.ykrasik.indexter.games.persistence.entity.ExcludedPathEntity;
import com.j256.ormlite.dao.Dao;

import java.nio.file.Path;
import java.sql.SQLException;

/**
 * @author Yevgeny Krasik
 */
public interface ExcludePathDao extends Dao<ExcludedPathEntity, Integer> {
    ExcludedPathEntity queryByPath(Path path) throws SQLException;
}
