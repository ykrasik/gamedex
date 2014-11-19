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
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.nio.file.Path;
import java.sql.SQLException;
import java.util.*;

/**
 * @author Yevgeny Krasik
 */
public class GameDataServiceImpl extends AbstractService implements GameDataService {
    private final PersistenceProperties properties;
    private final GameEntityTranslator translator;

    private volatile ObservableList<LocalGameInfo> cache = FXCollections.emptyObservableList();
    private volatile Map<Path, LocalGameInfo> cacheMap = Collections.emptyMap();
    private ObjectProperty<ObservableList<LocalGameInfo>> itemsProperty;

    private JdbcPooledConnectionSource connectionSource;
    private Dao<GameInfoEntity, String> gameInfoEntityDao;

    public GameDataServiceImpl(PersistenceProperties properties, GameEntityTranslator translator) {
        this.properties = Objects.requireNonNull(properties);
        this.translator = Objects.requireNonNull(translator);
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

        LOG.info("Fetching data...");
        final List<LocalGameInfo> infos = fetchData();
        this.cache = FXCollections.observableArrayList(infos);
        this.cacheMap = ListUtils.toMap(infos, LocalGameInfo::getPath);
        this.itemsProperty = new SimpleObjectProperty<>(cache);
    }

    private void prepareSchema() throws SQLException {
        TableUtils.createTableIfNotExists(connectionSource, GameInfoEntity.class);
    }

    @SuppressWarnings("unchecked")
    private void instantiateDaos() throws SQLException {
        gameInfoEntityDao = DaoManager.createDao(connectionSource, GameInfoEntity.class);
    }

    private List<LocalGameInfo> fetchData() throws SQLException {
        final List<GameInfoEntity> entities = gameInfoEntityDao.queryForAll();
        return ListUtils.map(entities, translator::translate);
    }

    @Override
    protected void doStop() throws Exception {
        connectionSource.close();
    }

    @Override
    public synchronized void add(LocalGameInfo info) throws DataException {
        // Insert to db.
        insert(info);

        // Update cache.
        cacheMap.put(info.getPath(), info);
        // FIXME: This possibly means that this list shouldn't be sitting here.
        Platform.runLater(() -> cache.add(info));
    }

    @Override
    public synchronized Optional<LocalGameInfo> get(Path path) throws DataException {
        return Optional.ofNullable(cacheMap.get(path));
    }

    @Override
    public synchronized ObservableList<LocalGameInfo> getAll() throws DataException {
        return FXCollections.unmodifiableObservableList(cache);
    }

    @Override
    public ReadOnlyObjectProperty<ObservableList<LocalGameInfo>> itemsProperty() {
        return itemsProperty;
    }

    private void insert(LocalGameInfo info) throws DataException {
        final GameInfoEntity entity = translator.translate(info);
        LOG.debug("Inserting {}...", entity);

        try {
            if (gameInfoEntityDao.create(entity) != 1) {
                throw new DataException("Error inserting entity: %s", entity);
            }
        } catch (SQLException e) {
            throw new DataException(e);
        }
    }
}
