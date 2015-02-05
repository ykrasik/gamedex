package com.github.ykrasik.indexter.games.persistence.dao;

import com.github.ykrasik.indexter.games.persistence.entity.GameEntity;
import com.github.ykrasik.indexter.games.persistence.entity.GenreEntity;
import com.github.ykrasik.indexter.games.persistence.entity.GenreGameLinkEntity;
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

    private GameDao gameDao;
    private GenreDao genreDao;
    private PreparedQuery<GenreEntity> fetchGenresByGameQuery;
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

    private PreparedDelete<GenreGameLinkEntity> prepareDeleteByGameIdQuery() throws SQLException {
        final DeleteBuilder<GenreGameLinkEntity, Integer> builder = deleteBuilder();
        builder.where().eq(GenreGameLinkEntity.GAME_COLUMN, gameArg);
        return builder.prepare();
    }

    @Override
    public List<GenreEntity> getGenresByGame(GameEntity game) throws SQLException {
        gameArg.setValue(game);
        return genreDao.query(fetchGenresByGameQuery);
    }

    @Override
    public void deleteByGameId(int gameId) throws SQLException {
        gameArg.setValue(gameId);
        delete(deleteByGameIdQuery);
    }
}
