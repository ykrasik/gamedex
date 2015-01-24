package com.github.ykrasik.indexter.games.persistence.dao;

import com.github.ykrasik.indexter.games.persistence.entity.LibraryEntity;
import com.j256.ormlite.dao.Dao;

import java.nio.file.Path;
import java.sql.SQLException;

/**
 * @author Yevgeny Krasik
 */
public interface LibraryDao extends Dao<LibraryEntity, Integer> {
    LibraryEntity queryByPath(Path path) throws SQLException;
}
