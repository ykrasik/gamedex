package com.github.ykrasik.indexter.games.persistence.dao;

import com.github.ykrasik.indexter.games.persistence.entity.LibraryGameLinkEntity;
import com.j256.ormlite.dao.BaseDaoImpl;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.stmt.PreparedDelete;
import com.j256.ormlite.stmt.SelectArg;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.DatabaseTableConfig;

import java.sql.SQLException;

/**
 * @author Yevgeny Krasik
 */
public class LibraryGameLinkDaoImpl extends BaseDaoImpl<LibraryGameLinkEntity, Integer> implements LibraryGameLinkDao {
    private final SelectArg gameArg = new SelectArg();
    private final SelectArg libraryArg = new SelectArg();
    private final PreparedDelete<LibraryGameLinkEntity> deleteByGameIdQuery = prepareDeleteByGameIdQuery();
    private final PreparedDelete<LibraryGameLinkEntity> deleteByLibraryIdQuery = prepareDeleteByLibraryIdQuery();

    public LibraryGameLinkDaoImpl(Class<LibraryGameLinkEntity> dataClass) throws SQLException {
        super(dataClass);
    }

    public LibraryGameLinkDaoImpl(ConnectionSource connectionSource, Class<LibraryGameLinkEntity> dataClass) throws SQLException {
        super(connectionSource, dataClass);
    }

    public LibraryGameLinkDaoImpl(ConnectionSource connectionSource, DatabaseTableConfig<LibraryGameLinkEntity> tableConfig) throws SQLException {
        super(connectionSource, tableConfig);
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
