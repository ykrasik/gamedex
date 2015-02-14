package com.github.ykrasik.gamedex.persistence.dao;

import com.github.ykrasik.gamedex.persistence.entity.LibraryEntity;
import com.github.ykrasik.gamedex.persistence.entity.LibraryGameLinkEntity;
import com.j256.ormlite.dao.Dao;

import java.sql.SQLException;
import java.util.List;

/**
 * @author Yevgeny Krasik
 */
public interface LibraryGameLinkDao extends Dao<LibraryGameLinkEntity, Integer> {
    List<LibraryEntity> getLibrariesByGameId(int gameId) throws SQLException;

    void deleteByGameId(int gameId) throws SQLException;
    void deleteByLibraryId(int libraryId) throws SQLException;
}
