package com.github.ykrasik.gamedex.persistence.dao.library;

import com.github.ykrasik.gamedex.persistence.dao.game.GameDao;
import com.github.ykrasik.gamedex.persistence.entity.GenreEntity;
import com.github.ykrasik.gamedex.persistence.entity.LibraryEntity;
import com.github.ykrasik.gamedex.persistence.entity.LibraryGameLinkEntity;
import com.gs.collections.api.list.ImmutableList;
import com.gs.collections.impl.factory.Lists;
import com.j256.ormlite.dao.BaseDaoImpl;
import com.j256.ormlite.stmt.*;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.DatabaseTableConfig;
import lombok.NonNull;

import java.sql.SQLException;

/**
 * @author Yevgeny Krasik
 */
public class LibraryGameLinkDaoImpl extends BaseDaoImpl<LibraryGameLinkEntity, Integer> implements LibraryGameLinkDao {
    public LibraryGameLinkDaoImpl(Class<LibraryGameLinkEntity> dataClass) throws SQLException {
        super(dataClass);
    }

    public LibraryGameLinkDaoImpl(ConnectionSource connectionSource, Class<LibraryGameLinkEntity> dataClass) throws SQLException {
        super(connectionSource, dataClass);
    }

    public LibraryGameLinkDaoImpl(ConnectionSource connectionSource, DatabaseTableConfig<LibraryGameLinkEntity> tableConfig) throws SQLException {
        super(connectionSource, tableConfig);
    }

    private final SelectArg gameArg = new SelectArg();
    private final SelectArg libraryArg = new SelectArg();
    private PreparedQuery<LibraryEntity> fetchLibrariesByGameQuery;
    private PreparedDelete<LibraryGameLinkEntity> deleteByGameIdQuery;
    private PreparedDelete<LibraryGameLinkEntity> deleteByLibraryIdQuery;

    private GameDao gameDao;
    private LibraryDao libraryDao;

    public void setDaos(@NonNull GameDao gameDao, @NonNull LibraryDao libraryDao) throws SQLException {
        this.gameDao = gameDao;
        this.libraryDao = libraryDao;

        this.fetchLibrariesByGameQuery = prepareFetchLibrariesByGameQuery();
        this.deleteByGameIdQuery = prepareDeleteByGameIdQuery();
        this.deleteByLibraryIdQuery = prepareDeleteByLibraryIdQuery();
    }

    private PreparedQuery<LibraryEntity> prepareFetchLibrariesByGameQuery() throws SQLException {
        // Inner query for LibraryGameLinkEntity.
        final QueryBuilder<LibraryGameLinkEntity, Integer> libraryGameLinkEntityQueryBuilder = queryBuilder();
        libraryGameLinkEntityQueryBuilder.selectColumns(LibraryGameLinkEntity.LIBRARY_COLUMN);
        libraryGameLinkEntityQueryBuilder.where().eq(LibraryGameLinkEntity.GAME_COLUMN, gameArg);

        // Outer query for Library.
        final QueryBuilder<LibraryEntity, Integer> libraryEntityQueryBuilder = libraryDao.queryBuilder();
        libraryEntityQueryBuilder.where().in(GenreEntity.ID_COLUMN, libraryGameLinkEntityQueryBuilder);
        return libraryEntityQueryBuilder.prepare();
    }

    private PreparedDelete<LibraryGameLinkEntity> prepareDeleteByGameIdQuery() throws SQLException {
        final DeleteBuilder<LibraryGameLinkEntity, Integer> builder = deleteBuilder();
        builder.where().eq(LibraryGameLinkEntity.GAME_COLUMN, gameArg);
        return builder.prepare();
    }

    private PreparedDelete<LibraryGameLinkEntity> prepareDeleteByLibraryIdQuery() throws SQLException {
        final DeleteBuilder<LibraryGameLinkEntity, Integer> builder = deleteBuilder();
        builder.where().eq(LibraryGameLinkEntity.LIBRARY_COLUMN, libraryArg);
        return builder.prepare();
    }

    @Override
    public ImmutableList<LibraryGameLinkEntity> getAll() throws SQLException {
        return Lists.immutable.ofAll(queryForAll());
    }

    @Override
    public ImmutableList<LibraryEntity> getLibrariesByGameId(int gameId) throws SQLException {
        gameArg.setValue(gameId);
        return Lists.immutable.ofAll(libraryDao.query(fetchLibrariesByGameQuery));
    }

    @Override
    public void deleteByGameId(int gameId) throws SQLException {
        gameArg.setValue(gameId);
        delete(deleteByGameIdQuery);
    }

    @Override
    public void deleteByLibraryId(int libraryId) throws SQLException {
        libraryArg.setValue(libraryId);
        delete(deleteByLibraryIdQuery);
    }
}
