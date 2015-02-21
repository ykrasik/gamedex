package com.github.ykrasik.gamedex.persistence.dao;

import com.github.ykrasik.gamedex.persistence.entity.LibraryEntity;
import com.github.ykrasik.gamedex.persistence.entity.LibraryGameLinkEntity;
import com.gs.collections.api.list.ImmutableList;
import com.j256.ormlite.dao.Dao;

import java.sql.SQLException;

/**
 * @author Yevgeny Krasik
 */
public interface LibraryGameLinkDao extends Dao<LibraryGameLinkEntity, Integer> {
    ImmutableList<LibraryGameLinkEntity> getAll() throws SQLException;

    ImmutableList<LibraryEntity> getLibrariesByGameId(int gameId) throws SQLException;

    void deleteByGameId(int gameId) throws SQLException;
    void deleteByLibraryId(int libraryId) throws SQLException;
}
