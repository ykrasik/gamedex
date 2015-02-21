package com.github.ykrasik.gamedex.persistence.dao;

import com.github.ykrasik.gamedex.persistence.entity.LibraryEntity;
import com.gs.collections.api.list.ImmutableList;
import com.j256.ormlite.dao.Dao;

import java.nio.file.Path;
import java.sql.SQLException;

/**
 * @author Yevgeny Krasik
 */
public interface LibraryDao extends Dao<LibraryEntity, Integer> {
    ImmutableList<LibraryEntity> getAll() throws SQLException;

    LibraryEntity queryByPath(Path path) throws SQLException;
}
