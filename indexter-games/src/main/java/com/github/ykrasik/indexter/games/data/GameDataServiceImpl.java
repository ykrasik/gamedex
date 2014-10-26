package com.github.ykrasik.indexter.games.data;

import com.github.ykrasik.indexter.AbstractService;
import com.github.ykrasik.indexter.exception.DataException;
import com.github.ykrasik.indexter.games.data.config.PersistenceProperties;
import com.github.ykrasik.indexter.games.data.entity.GameInfoEntity;
import com.github.ykrasik.indexter.games.data.translator.GameEntityTranslator;
import com.github.ykrasik.indexter.games.datamodel.LocalGameInfo;
import com.github.ykrasik.indexter.util.ListUtils;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcPooledConnectionSource;
import com.j256.ormlite.table.TableUtils;

import java.nio.file.Path;
import java.sql.SQLException;
import java.util.*;

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
    public void add(LocalGameInfo info) throws DataException {
        doAdd(info);
        notifyListeners(Collections.singleton(info));
    }

    @Override
    public Optional<LocalGameInfo> get(Path path) throws DataException {
        try {
            final Optional<GameInfoEntity> entity = Optional.ofNullable(gameInfoEntityDao.queryForId(path.toString()));
            return entity.map(translator::translate);
        } catch (SQLException e) {
            throw new DataException(e);
        }
    }

    @Override
    public Collection<LocalGameInfo> getAll() throws DataException {
        try {
            final List<GameInfoEntity> entities = gameInfoEntityDao.queryForAll();
            return ListUtils.map(entities, translator::translate);
        } catch (SQLException e) {
            throw new DataException(e);
        }
    }

    private void doAdd(LocalGameInfo info) throws DataException {
        final GameInfoEntity entity = translator.translate(info);
        LOG.debug("Inserting {}...", entity);

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

    private void notifyListeners(Collection<LocalGameInfo> newOrUpdatedInfos) {
        for (GameDataListener listener : listeners) {
            listener.onUpdate(newOrUpdatedInfos);
        }
    }
}
