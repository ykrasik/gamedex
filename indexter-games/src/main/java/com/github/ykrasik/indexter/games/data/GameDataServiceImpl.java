package com.github.ykrasik.indexter.games.data;

import com.github.ykrasik.indexter.AbstractService;
import com.github.ykrasik.indexter.exception.DataException;
import com.github.ykrasik.indexter.exception.ExceptionWrappers;
import com.github.ykrasik.indexter.games.data.config.PersistenceProperties;
import com.github.ykrasik.indexter.games.data.entity.GameInfoEntity;
import com.github.ykrasik.indexter.games.data.translator.GameEntityTranslator;
import com.github.ykrasik.indexter.games.datamodel.GameInfo;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcPooledConnectionSource;
import com.j256.ormlite.table.TableUtils;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Yevgeny Krasik
 */
public class GameDataServiceImpl extends AbstractService implements GameDataService {
    private final PersistenceProperties properties;
    private final GameEntityTranslator translator;
    private final List<GameDataListener> listeners;

    private JdbcPooledConnectionSource connectionSource;
    private Dao<GameInfoEntity, String> gameInfoEntityDao;

    public GameDataServiceImpl(PersistenceProperties properties, GameEntityTranslator translator) {
        this.properties = Objects.requireNonNull(properties);
        this.translator = Objects.requireNonNull(translator);
        this.listeners = new ArrayList<>();
    }

    @Override
    protected void doStart() throws Exception {
        final String dbUrl = properties.getDbUrl();
        LOG.debug("Connection url: {}", dbUrl);
        connectionSource = new JdbcPooledConnectionSource(dbUrl);
        connectionSource.setCheckConnectionsEveryMillis(properties.getConnectionCheckInterval().toMillis());

        LOG.debug("Preparing schema...");
        prepareSchema();

        LOG.debug("Instantiating DAOs...");
        instantiateDaos();
    }

    private void prepareSchema() throws SQLException {
        TableUtils.createTableIfNotExists(connectionSource, GameInfoEntity.class);
    }

    @SuppressWarnings("unchecked")
    private void instantiateDaos() throws SQLException {
        gameInfoEntityDao = DaoManager.createDao(connectionSource, GameInfoEntity.class);
    }

    @Override
    protected void doStop() throws Exception {
        connectionSource.close();
    }

    @Override
    public void add(GameInfo info) throws DataException {
        doAdd(info);
        notifyListeners(Collections.singleton(info));
    }

    @Override
    public void addAll(Collection<GameInfo> infos) throws DataException {
        infos.forEach(ExceptionWrappers.rethrow(this::doAdd));
        notifyListeners(infos);
    }

    @Override
    public Collection<GameInfo> getAll() throws DataException {
        try {
            final List<GameInfoEntity> entities = gameInfoEntityDao.queryForAll();
            return entities.stream()
                .map(translator::translate)
                .collect(Collectors.toList());
        } catch (SQLException e) {
            throw new DataException(e);
        }
    }

    private void doAdd(GameInfo info) throws DataException {
        final GameInfoEntity entity = translator.translate(info);
        try {
            if (gameInfoEntityDao.create(entity) != 1) {
                throw new DataException("Error inserting entity: " + entity);
            }
        } catch (SQLException e) {
            throw new DataException(e);
        }
    }

    @Override
    public void addListener(GameDataListener listener) {
        listeners.add(listener);
    }

    private void notifyListeners(Collection<GameInfo> newOrUpdatedInfos) {
        for (GameDataListener listener : listeners) {
            listener.onUpdate(newOrUpdatedInfos);
        }
    }
}
