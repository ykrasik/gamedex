package com.github.ykrasik.gamedex.persistence.dao;

import com.github.ykrasik.gamedex.persistence.entity.GenreEntity;
import com.github.ykrasik.gamedex.persistence.entity.GenreGameLinkEntity;
import com.j256.ormlite.dao.Dao;

import java.sql.SQLException;
import java.util.List;

/**
 * @author Yevgeny Krasik
 */
public interface GenreGameLinkDao extends Dao<GenreGameLinkEntity, Integer> {
    List<GenreGameLinkEntity> getByGenreId(int genreId) throws SQLException;

    List<GenreEntity> getGenresByGameId(int gameId) throws SQLException;

    void deleteByGameId(int gameId) throws SQLException;
}
