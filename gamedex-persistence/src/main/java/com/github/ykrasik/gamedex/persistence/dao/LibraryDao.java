package com.github.ykrasik.gamedex.persistence.dao;

import com.github.ykrasik.gamedex.persistence.entity.LibraryEntity;
import com.j256.ormlite.dao.Dao;

import java.nio.file.Path;
import java.sql.SQLException;

/**
 * @author Yevgeny Krasik
 */
public interface LibraryDao extends Dao<LibraryEntity, Integer> {
    LibraryEntity queryByPath(Path path) throws SQLException;
}
