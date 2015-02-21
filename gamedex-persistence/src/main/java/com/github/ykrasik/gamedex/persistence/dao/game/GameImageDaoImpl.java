package com.github.ykrasik.gamedex.persistence.dao.game;

import com.github.ykrasik.gamedex.persistence.entity.GameImageEntity;
import com.github.ykrasik.gamedex.persistence.entity.GameImageEntityType;
import com.github.ykrasik.opt.Opt;
import com.j256.ormlite.dao.BaseDaoImpl;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.stmt.PreparedDelete;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.SelectArg;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.DatabaseTableConfig;

import java.sql.SQLException;

/**
 * @author Yevgeny Krasik
 */
public class GameImageDaoImpl extends BaseDaoImpl<GameImageEntity, Void> implements GameImageDao {
    public GameImageDaoImpl(Class<GameImageEntity> dataClass) throws SQLException {
        super(dataClass);
    }

    public GameImageDaoImpl(ConnectionSource connectionSource, Class<GameImageEntity> dataClass) throws SQLException {
        super(connectionSource, dataClass);
    }

    public GameImageDaoImpl(ConnectionSource connectionSource, DatabaseTableConfig<GameImageEntity> tableConfig) throws SQLException {
        super(connectionSource, tableConfig);
    }

    private final SelectArg gameIdArg = new SelectArg();
    private final SelectArg typeArg = new SelectArg();
    private final PreparedQuery<GameImageEntity> gameQuery = queryBuilder().where()
        .eq(GameImageEntity.GAME_ID_COLUMN, gameIdArg).and()
        .eq(GameImageEntity.TYPE_COLUMN, typeArg)
        .prepare();

    private final PreparedDelete<GameImageEntity> deleteByGameIdQuery = prepareDeleteByGameIdQuery();

    private PreparedDelete<GameImageEntity> prepareDeleteByGameIdQuery() throws SQLException {
        final DeleteBuilder<GameImageEntity, Void> builder = deleteBuilder();
        builder.where().eq(GameImageEntity.GAME_ID_COLUMN, gameIdArg);
        return builder.prepare();
    }

    @Override
    public Opt<GameImageEntity> queryByGameId(int gameId, GameImageEntityType type) throws SQLException {
        gameIdArg.setValue(gameId);
        typeArg.setValue(type);
        return Opt.ofNullable(queryForFirst(gameQuery));
    }

    @Override
    public void deleteByGameId(int gameId) throws SQLException {
        gameIdArg.setValue(gameId);
        delete(deleteByGameIdQuery);
    }
}
