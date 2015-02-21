package com.github.ykrasik.gamedex.persistence.dao.genre;

import com.github.ykrasik.gamedex.persistence.entity.GenreEntity;
import com.github.ykrasik.gamedex.persistence.entity.GenreGameLinkEntity;
import com.gs.collections.api.list.ImmutableList;
import com.j256.ormlite.dao.Dao;

import java.sql.SQLException;

/**
 * @author Yevgeny Krasik
 */
public interface GenreGameLinkDao extends Dao<GenreGameLinkEntity, Integer> {
    ImmutableList<GenreGameLinkEntity> getAll() throws SQLException;

    ImmutableList<GenreGameLinkEntity> getByGenreId(int genreId) throws SQLException;

    ImmutableList<GenreEntity> getGenresByGameId(int gameId) throws SQLException;

    void deleteByGameId(int gameId) throws SQLException;
}
