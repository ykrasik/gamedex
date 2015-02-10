package com.github.ykrasik.gamedex.persistence.dao;

import com.github.ykrasik.gamedex.persistence.entity.GenreEntity;
import com.github.ykrasik.gamedex.persistence.entity.GenreGameLinkEntity;
import com.j256.ormlite.dao.BaseDaoImpl;
import com.j256.ormlite.stmt.*;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.DatabaseTableConfig;
import lombok.NonNull;

import java.sql.SQLException;
import java.util.List;

/**
 * @author Yevgeny Krasik
 */
public class GenreGameLinkDaoImpl extends BaseDaoImpl<GenreGameLinkEntity, Integer> implements GenreGameLinkDao {
    private final SelectArg gameArg = new SelectArg();
    private final SelectArg genreArg = new SelectArg();

    private GameDao gameDao;
    private GenreDao genreDao;

    private PreparedQuery<GenreEntity> fetchGenresByGameQuery;
    private PreparedQuery<GenreGameLinkEntity> fetchByGenreIdQuery;
    private PreparedDelete<GenreGameLinkEntity> deleteByGameIdQuery;

    public GenreGameLinkDaoImpl(Class<GenreGameLinkEntity> dataClass) throws SQLException {
        super(dataClass);
    }

    public GenreGameLinkDaoImpl(ConnectionSource connectionSource, Class<GenreGameLinkEntity> dataClass) throws SQLException {
        super(connectionSource, dataClass);
    }

    public GenreGameLinkDaoImpl(ConnectionSource connectionSource, DatabaseTableConfig<GenreGameLinkEntity> tableConfig) throws SQLException {
        super(connectionSource, tableConfig);
    }

    public void setDaos(@NonNull GameDao gameDao, @NonNull GenreDao genreDao) throws SQLException {
        this.gameDao = gameDao;
        this.genreDao = genreDao;

        this.fetchGenresByGameQuery = prepareFetchGenresByGameQuery();
        this.fetchByGenreIdQuery = prepareFetchByGenreIdQuery();
        this.deleteByGameIdQuery = prepareDeleteByGameIdQuery();
    }

    private PreparedQuery<GenreEntity> prepareFetchGenresByGameQuery() throws SQLException {
        // Inner query for GenreGameLinkEntity.
        final QueryBuilder<GenreGameLinkEntity, Integer> genreGameLinkEntityQueryBuilder = queryBuilder();
        genreGameLinkEntityQueryBuilder.selectColumns(GenreGameLinkEntity.GENRE_COLUMN);
        genreGameLinkEntityQueryBuilder.where().eq(GenreGameLinkEntity.GAME_COLUMN, gameArg);

        // Outer query for Genre.
        final QueryBuilder<GenreEntity, Integer> genreEntityQueryBuilder = genreDao.queryBuilder();
        genreEntityQueryBuilder.where().in(GenreEntity.ID_COLUMN, genreGameLinkEntityQueryBuilder);
        return genreEntityQueryBuilder.prepare();
    }

    private PreparedQuery<GenreGameLinkEntity> prepareFetchByGenreIdQuery() throws SQLException {
        return queryBuilder().where().eq(GenreGameLinkEntity.GENRE_COLUMN, genreArg).prepare();
    }

    private PreparedDelete<GenreGameLinkEntity> prepareDeleteByGameIdQuery() throws SQLException {
        final DeleteBuilder<GenreGameLinkEntity, Integer> builder = deleteBuilder();
        builder.where().eq(GenreGameLinkEntity.GAME_COLUMN, gameArg);
        return builder.prepare();
    }

    @Override
    public List<GenreGameLinkEntity> getByGenreId(int genreId) throws SQLException {
        genreArg.setValue(genreId);
        return query(fetchByGenreIdQuery);
    }

    @Override
    public List<GenreEntity> getGenresByGameId(int gameId) throws SQLException {
        gameArg.setValue(gameId);
        return genreDao.query(fetchGenresByGameQuery);
    }

    @Override
    public void deleteByGameId(int gameId) throws SQLException {
        gameArg.setValue(gameId);
        delete(deleteByGameIdQuery);
    }
}
