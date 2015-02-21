package com.github.ykrasik.gamedex.persistence;

import com.github.ykrasik.gamedex.common.service.AbstractService;
import com.github.ykrasik.gamedex.datamodel.GamePlatform;
import com.github.ykrasik.gamedex.datamodel.persistence.*;
import com.github.ykrasik.gamedex.datamodel.provider.UnifiedGameInfo;
import com.github.ykrasik.gamedex.persistence.config.PersistenceProperties;
import com.github.ykrasik.gamedex.persistence.dao.*;
import com.github.ykrasik.gamedex.persistence.entity.*;
import com.github.ykrasik.gamedex.persistence.exception.DataException;
import com.github.ykrasik.gamedex.persistence.extractor.GenreExtractor;
import com.github.ykrasik.gamedex.persistence.extractor.LibraryExtractor;
import com.github.ykrasik.gamedex.persistence.translator.exclude.ExcludedPathEntityTranslator;
import com.github.ykrasik.gamedex.persistence.translator.game.GameEntityTranslator;
import com.github.ykrasik.gamedex.persistence.translator.genre.GenreEntityTranslator;
import com.github.ykrasik.gamedex.persistence.translator.library.LibraryEntityTranslator;
import com.gs.collections.api.list.ImmutableList;
import com.gs.collections.api.list.MutableList;
import com.gs.collections.api.map.ImmutableMap;
import com.gs.collections.api.multimap.list.ImmutableListMultimap;
import com.gs.collections.impl.factory.Lists;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcPooledConnectionSource;
import com.j256.ormlite.table.TableUtils;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import java.nio.file.Path;
import java.sql.SQLException;

/**
 * @author Yevgeny Krasik
 */
@RequiredArgsConstructor
public class PersistenceServiceImpl extends AbstractService implements PersistenceService {
    @NonNull private final PersistenceProperties properties;
    @NonNull private final GameEntityTranslator gameTranslator;
    @NonNull private final GenreEntityTranslator genreTranslator;
    @NonNull private final LibraryEntityTranslator libraryTranslator;
    @NonNull private final ExcludedPathEntityTranslator excludedPathTranslator;

    private JdbcPooledConnectionSource connectionSource;
    private GameDao gameDao;
    private GenreDao genreDao;
    private GenreGameLinkDao genreGameLinkDao;
    private LibraryDao libraryDao;
    private LibraryGameLinkDao libraryGameLinkDao;
    private ExcludePathDao excludedPathDao;

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
        TableUtils.createTableIfNotExists(connectionSource, GenreEntity.class);
        TableUtils.createTableIfNotExists(connectionSource, GenreGameLinkEntity.class);
        TableUtils.createTableIfNotExists(connectionSource, LibraryEntity.class);
        TableUtils.createTableIfNotExists(connectionSource, LibraryGameLinkEntity.class);
        TableUtils.createTableIfNotExists(connectionSource, ExcludedPathEntity.class);
    }

    @SuppressWarnings("unchecked")
    private void instantiateDaos() throws SQLException {
        gameDao = DaoManager.createDao(connectionSource, GameEntity.class);
        genreDao = DaoManager.createDao(connectionSource, GenreEntity.class);
        genreGameLinkDao = DaoManager.createDao(connectionSource, GenreGameLinkEntity.class);
        ((GenreGameLinkDaoImpl)genreGameLinkDao).setDaos(gameDao, genreDao);
        libraryDao = DaoManager.createDao(connectionSource, LibraryEntity.class);
        libraryGameLinkDao = DaoManager.createDao(connectionSource, LibraryGameLinkEntity.class);
        ((LibraryGameLinkDaoImpl)libraryGameLinkDao).setDaos(gameDao, libraryDao);
        excludedPathDao = DaoManager.createDao(connectionSource, ExcludedPathEntity.class);
    }

    @Override
    protected void doStop() throws Exception {
        connectionSource.close();
    }

    @Override
    @SneakyThrows
    public Game addGame(UnifiedGameInfo gameInfo, Path path, GamePlatform platform) {
        // Insert game.
        final GameEntity game = gameTranslator.translate(gameInfo);
        game.setPath(path.toString());
        game.setPlatform(platform);
        LOG.debug("Inserting {}...", game);
        if (gameDao.create(game) != 1) {
            throw new DataException("Error inserting game: %s", gameInfo);
        }

        // Insert all new genres.
        final ImmutableList<GenreEntity> genresEntities = gameInfo.getGenres().collect(this::getOrCreateGenreByName);

        // Link genres to game.
        for (GenreEntity genre : genresEntities) {
            final GenreGameLinkEntity genreGameLinkEntity = new GenreGameLinkEntity();
            genreGameLinkEntity.setGame(game);
            genreGameLinkEntity.setGenre(genre);
            genreGameLinkDao.create(genreGameLinkEntity);
        }

        return getGameById(new Id<>(game.getId()));
    }

    @SneakyThrows
    private GenreEntity getOrCreateGenreByName(String name) {
        return genreDao.getOrCreateByName(name);
    }

    @Override
    @SneakyThrows
    public void deleteGame(Id<Game> id) {
        final int gameId = id.getId();
        LOG.debug("Deleting game with id {}...", gameId);
        if (gameDao.deleteById(gameId) != 1) {
            throw new DataException("Error deleting game: %d. Entry doesn't exist?", gameId);
        }

        final ImmutableList<GenreEntity> genres = genreGameLinkDao.getGenresByGameId(gameId);
        genreGameLinkDao.deleteByGameId(gameId);

        // Delete any genres which were only linked to this game.
        final MutableList<GenreEntity> genresToDelete = Lists.mutable.of();
        for (GenreEntity genre : genres) {
            final ImmutableList<GenreGameLinkEntity> genreLinks = genreGameLinkDao.getByGenreId(genre.getId());
            if (genreLinks.isEmpty()) {
                LOG.debug("Genre={} was only linked to gameId={}, deleting...", genre, gameId);
                genresToDelete.add(genre);
            }
        }
        if (!genresToDelete.isEmpty()) {
            genreDao.deleteIds(genresToDelete.collect(GenreEntity::getId));
        }

        libraryGameLinkDao.deleteByGameId(gameId);
    }

    @Override
    @SneakyThrows
    public ImmutableList<Game> getAllGames() {
        LOG.debug("Fetching all games...");
        final ImmutableList<GameEntity> games = Lists.immutable.ofAll(gameDao.queryForAll());

        // This is required due to lack of support for intelligent joins in OrmLite... :/
        final GenreExtractor genreExtractor = createGenreExtractor();
        final LibraryExtractor libraryExtractor = createLibraryExtractor();
        return games.collect(game -> translateGame(game, genreExtractor, libraryExtractor));
    }

    private GenreExtractor createGenreExtractor() throws SQLException {
        final ImmutableList<Genre> genres = getAllGenres();
        final ImmutableMap<Id<Genre>, Genre> genreMap = genres.groupByUniqueKey(Genre::getId);

        final ImmutableList<GenreGameLinkEntity> genreGames = genreGameLinkDao.getAll();
        final ImmutableListMultimap<Integer, GenreGameLinkEntity> genreGameMap = genreGames.groupBy(entity -> entity.getGame().getId());

        return new GenreExtractor(genreMap, genreGameMap);
    }

    private LibraryExtractor createLibraryExtractor() throws SQLException {
        final ImmutableList<Library> libraries = getAllLibraries();
        final ImmutableMap<Id<Library>, Library> libraryMap = libraries.groupByUniqueKey(Library::getId);

        final ImmutableList<LibraryGameLinkEntity> libraryGames = libraryGameLinkDao.getAll();
        final ImmutableListMultimap<Integer, LibraryGameLinkEntity> libraryGameMap = libraryGames.groupBy(entity -> entity.getGame().getId());

        return new LibraryExtractor(libraryMap, libraryGameMap);
    }

    private Game translateGame(GameEntity entity, GenreExtractor genreExtractor, LibraryExtractor libraryExtractor) {
        final ImmutableList<Genre> genres = genreExtractor.getData(entity.getId()).toList().toImmutable();
        final ImmutableList<Library> libraries = libraryExtractor.getData(entity.getId()).toList().toImmutable();
        return gameTranslator.translate(entity, genres, libraries);
    }

    @Override
    @SneakyThrows
    public Game getGameById(Id<Game> id) {
        final GameEntity entity = gameDao.queryForId(id.getId());
        if (entity == null) {
            throw new DataException("Couldn't find game with id: %d", id);
        }
        final ImmutableList<Genre> genres = getGenresByGame(entity);
        final ImmutableList<Library> libraries = getLibrariesByGame(entity);
        return gameTranslator.translate(entity, genres, libraries);
    }

    private ImmutableList<Genre> getGenresByGame(GameEntity game) throws SQLException {
        final ImmutableList<GenreEntity> entities = genreGameLinkDao.getGenresByGameId(game.getId());
        return entities.collect(genreTranslator::translate);
    }

    private ImmutableList<Library> getLibrariesByGame(GameEntity entity) throws SQLException {
        final ImmutableList<LibraryEntity> entities = libraryGameLinkDao.getLibrariesByGameId(entity.getId());
        return entities.collect(libraryTranslator::translate);
    }

    @Override
    @SneakyThrows
    public boolean hasGameForPath(Path path) {
        return gameDao.queryByPath(path) != null;
    }

    @Override
    @SneakyThrows
    public Library addLibrary(Path path, GamePlatform platform, String name) {
        final LibraryEntity entity = new LibraryEntity();
        entity.setPath(path.toString());
        entity.setPlatform(platform);
        entity.setName(name);

        LOG.debug("Inserting {}...", entity);
        if (libraryDao.create(entity) != 1) {
            throw new DataException("Error inserting library: %s", entity);
        }
        return libraryTranslator.translate(entity);
    }

    @Override
    @SneakyThrows
    public void deleteLibrary(Id<Library> id) {
        final int libraryId = id.getId();
        LOG.debug("Deleting library of id {}...", libraryId);
        if (libraryDao.deleteById(libraryId) != 1) {
            throw new DataException("Error deleting library by id: %s. Entry doesn't exist?", libraryId);
        }
        libraryGameLinkDao.deleteByLibraryId(libraryId);
    }

    @Override
    @SneakyThrows
    public ImmutableList<Library> getAllLibraries() {
        LOG.debug("Fetching all libraries...");
        final ImmutableList<LibraryEntity> entities = libraryDao.getAll();
        return entities.collect(libraryTranslator::translate);
    }

    @Override
    @SneakyThrows
    public Library getLibraryById(Id<Library> id) {
        final LibraryEntity entity = libraryDao.queryForId(id.getId());
        if (entity == null) {
            throw new DataException("Couldn't find library with id: %d", id);
        }
        return libraryTranslator.translate(entity);
    }

    @Override
    @SneakyThrows
    public boolean hasLibraryForPath(Path path) {
        return libraryDao.queryByPath(path) != null;
    }

    @Override
    @SneakyThrows
    public void addGameToLibraries(Game game, Iterable<Library> libraries) {
        for (Library library : libraries) {
            addGameToLibrary(game, library);
        }
    }

    private void addGameToLibrary(Game game, Library library) throws SQLException {
        LOG.debug("Adding game {} to library {}...", game, library);
        final GameEntity gameEntity = gameTranslator.translate(game);
        final LibraryEntity libraryEntity = libraryTranslator.translate(library);
        final LibraryGameLinkEntity linkEntity = new LibraryGameLinkEntity();
        linkEntity.setGame(gameEntity);
        linkEntity.setLibrary(libraryEntity);
        if (libraryGameLinkDao.create(linkEntity) != 1) {
            throw new DataException("Error adding game to library! Game=%s, Library=%s", game, library);
        }
    }

    @Override
    @SneakyThrows
    public ImmutableList<Genre> getAllGenres() {
        LOG.debug("Fetching all genres...");
        final ImmutableList<GenreEntity> entities = genreDao.getAll();
        return entities.collect(genreTranslator::translate);
    }

    @Override
    @SneakyThrows
    public ImmutableList<ExcludedPath> getAllExcludedPaths() {
        final ImmutableList<ExcludedPathEntity> entities = excludedPathDao.getAll();
        return entities.collect(excludedPathTranslator::translate);
    }

    @Override
    @SneakyThrows
    public boolean isPathExcluded(Path path) {
        return excludedPathDao.queryByPath(path) != null;
    }

    @Override
    @SneakyThrows
    public ExcludedPath addExcludedPath(Path path) {
        final ExcludedPathEntity entity = new ExcludedPathEntity();
        entity.setPath(path.toString());

        LOG.debug("Inserting {}...", entity);
        if (excludedPathDao.create(entity) != 1) {
            throw new DataException("Error inserting excluded path: %s", entity);
        }
        return excludedPathTranslator.translate(entity);
    }

    @Override
    @SneakyThrows
    public void deleteExcludedPath(Id<ExcludedPath> id) {
        final int excludedPathId = id.getId();
        LOG.debug("Deleting excluded path of id {}...", excludedPathId);
        if (excludedPathDao.deleteById(excludedPathId) != 1) {
            throw new DataException("Error deleting excluded path by id: %s. Entry doesn't exist?", excludedPathId);
        }
    }
}
