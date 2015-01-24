package com.github.ykrasik.indexter.games.persistence;

import com.github.ykrasik.indexter.AbstractService;
import com.github.ykrasik.indexter.exception.DataException;
import com.github.ykrasik.indexter.games.datamodel.Game;
import com.github.ykrasik.indexter.games.datamodel.Library;
import com.github.ykrasik.indexter.games.datamodel.LocalGame;
import com.github.ykrasik.indexter.games.datamodel.LocalLibrary;
import com.github.ykrasik.indexter.games.persistence.config.PersistenceProperties;
import com.github.ykrasik.indexter.games.persistence.dao.GameDao;
import com.github.ykrasik.indexter.games.persistence.dao.LibraryDao;
import com.github.ykrasik.indexter.games.persistence.entity.GameEntity;
import com.github.ykrasik.indexter.games.persistence.entity.LibraryEntity;
import com.github.ykrasik.indexter.games.persistence.entity.LibraryGameLinkEntity;
import com.github.ykrasik.indexter.games.persistence.translator.GameEntityTranslator;
import com.github.ykrasik.indexter.games.persistence.translator.LibraryEntityTranslator;
import com.github.ykrasik.indexter.id.Id;
import com.github.ykrasik.indexter.util.DateUtils;
import com.github.ykrasik.indexter.util.ListUtils;
import com.github.ykrasik.indexter.util.RunnableThrows;
import com.github.ykrasik.indexter.util.SupplierThrows;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcPooledConnectionSource;
import com.j256.ormlite.table.TableUtils;

import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * @author Yevgeny Krasik
 */
public class PersistenceServiceImpl extends AbstractService implements PersistenceService {
    private final PersistenceProperties properties;
    private final GameEntityTranslator gameTranslator;
    private final LibraryEntityTranslator libraryTranslator;

    private JdbcPooledConnectionSource connectionSource;
    private GameDao gameDao;
    private LibraryDao libraryDao;
    private Dao<LibraryGameLinkEntity, Integer> libraryGameLinkDao;

    public PersistenceServiceImpl(PersistenceProperties properties,
                                  GameEntityTranslator gameTranslator,
                                  LibraryEntityTranslator libraryTranslator) {
        this.properties = Objects.requireNonNull(properties);
        this.gameTranslator = Objects.requireNonNull(gameTranslator);
        this.libraryTranslator = Objects.requireNonNull(libraryTranslator);
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
        TableUtils.createTableIfNotExists(connectionSource, GameEntity.class);
        TableUtils.createTableIfNotExists(connectionSource, LibraryEntity.class);
        TableUtils.createTableIfNotExists(connectionSource, LibraryGameLinkEntity.class);
    }

    @SuppressWarnings("unchecked")
    private void instantiateDaos() throws SQLException {
        gameDao = DaoManager.createDao(connectionSource, GameEntity.class);
        libraryDao = DaoManager.createDao(connectionSource, LibraryEntity.class);
        libraryGameLinkDao = DaoManager.createDao(connectionSource, LibraryGameLinkEntity.class);
    }

    @Override
    protected void doStop() throws Exception {
        connectionSource.close();
    }

    @Override
    public LocalGame addGame(Game game, Path path) throws DataException {
        return wrapException(() -> {
            final GameEntity entity = gameTranslator.translate(game, path);

            LOG.debug("Inserting {}...", entity);
            if (gameDao.create(entity) != 1) {
                throw new DataException("Error inserting game: %s", game);
            }

            return new LocalGame(new Id<>(entity.getId()), path, DateUtils.toLocalDateTime(entity.getLastModified()), game);
        });
    }

    @Override
    public void deleteGame(Id<LocalGame> id) throws DataException {
        wrapException(() -> {
            LOG.debug("Deleting game with id {}...", id);
            if (gameDao.deleteById(id.getId()) != 1) {
                throw new DataException("Error deleting game: %d. Entry doesn't exist?", id);
            }
        });
    }

    @Override
    public LocalGame getGameById(Id<LocalGame> id) throws DataException {
        return wrapException(() -> {
            final GameEntity entity = gameDao.queryForId(id.getId());
            if (entity == null) {
                throw new DataException("Couldn't find game with id: %d", id);
            }
            return gameTranslator.translate(entity);
        });
    }

    @Override
    public Optional<LocalGame> getGameByPath(Path path) throws DataException {
        return wrapException(() -> {
            final GameEntity game = gameDao.queryByPath(path);
            return Optional.ofNullable(game).map(gameTranslator::translate);
        });
    }

    @Override
    public List<LocalGame> getAllGames() throws DataException {
        return wrapException(() -> {
            LOG.debug("Fetching all games...");
            final List<GameEntity> entities = gameDao.queryForAll();
            return ListUtils.map(entities, gameTranslator::translate);
        });
    }

    @Override
    public LocalLibrary addLibrary(Library library) throws DataException {
        return wrapException(() -> {
            final LibraryEntity entity = libraryTranslator.translate(library);

            LOG.debug("Inserting {}...", entity);
            if (libraryDao.create(entity) != 1) {
                throw new DataException("Error inserting library: %s", entity);
            }

            return new LocalLibrary(new Id<>(entity.getId()), library);
        });
    }

    @Override
    public void deleteLibrary(Id<LocalLibrary> id) throws DataException {
        wrapException(() -> {
            LOG.debug("Deleting library of id {}...", id);
            if (libraryDao.deleteById(id.getId()) != 1) {
                throw new DataException("Error deleting library by id: %s. Entry doesn't exist?", id);
            }
        });
    }

    @Override
    public LocalLibrary getLibraryById(Id<LocalLibrary> id) throws DataException {
        return wrapException(() -> {
            final LibraryEntity entity = libraryDao.queryForId(id.getId());
            if (entity == null) {
                throw new DataException("Couldn't find library with id: %d", id);
            }
            return libraryTranslator.translate(entity);
        });
    }

    @Override
    public Optional<LocalLibrary> getLibraryByPath(Path path) throws DataException {
        return wrapException(() -> {
            final LibraryEntity library = libraryDao.queryByPath(path);
            return Optional.ofNullable(library).map(libraryTranslator::translate);
        });
    }

    @Override
    public List<LocalLibrary> getAllLibraries() throws DataException {
        return wrapException(() -> {
            LOG.debug("Fetching all libraries...");
            final List<LibraryEntity> entities = libraryDao.queryForAll();
            return ListUtils.map(entities, libraryTranslator::translate);
        });
    }

    @Override
    public void addGameToLibrary(LocalGame game, LocalLibrary library) {
        wrapException(() -> {
            LOG.debug("Adding game {} to library {}...", game, library);
            final GameEntity gameEntity = gameTranslator.translate(game);
            final LibraryEntity libraryEntity = libraryTranslator.translate(library);
            final LibraryGameLinkEntity linkEntity = new LibraryGameLinkEntity(libraryEntity, gameEntity);
            if (libraryGameLinkDao.create(linkEntity) != 1) {
                throw new DataException("Error adding game to library! Game=%s, Library=%s", game, library);
            }
        });
    }

    private void wrapException(RunnableThrows runnable) {
        try {
            runnable.run();
        } catch (Exception e) {
            throw new DataException(e);
        }
    }

    private <T> T wrapException(SupplierThrows<T> supplier) {
        try {
            return supplier.get();
        } catch (Exception e) {
            throw new DataException(e);
        }
    }
}
