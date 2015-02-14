package com.github.ykrasik.gamedex.persistence;

import com.github.ykrasik.gamedex.common.service.AbstractService;
import com.github.ykrasik.gamedex.common.util.ListUtils;
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
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcPooledConnectionSource;
import com.j256.ormlite.table.TableUtils;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import java.nio.file.Path;
import java.sql.SQLException;
import java.util.*;

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
        final List<GenreEntity> genresEntities = ListUtils.mapX(gameInfo.getGenres(), genreDao::getOrCreateByName);

        // Link genres to game.
        for (GenreEntity genre : genresEntities) {
            final GenreGameLinkEntity genreGameLinkEntity = new GenreGameLinkEntity();
            genreGameLinkEntity.setGame(game);
            genreGameLinkEntity.setGenre(genre);
            genreGameLinkDao.create(genreGameLinkEntity);
        }

        return getGameById(new Id<>(game.getId()));
    }

    @Override
    @SneakyThrows
    public void deleteGame(Id<Game> id) {
        final int gameId = id.getId();
        LOG.debug("Deleting game with id {}...", gameId);
        if (gameDao.deleteById(gameId) != 1) {
            throw new DataException("Error deleting game: %d. Entry doesn't exist?", gameId);
        }

        final List<GenreEntity> genres = genreGameLinkDao.getGenresByGameId(gameId);
        genreGameLinkDao.deleteByGameId(gameId);

        // Delete any genres which were only linked to this game.
        final List<GenreEntity> genresToDelete = new LinkedList<>();
        for (GenreEntity genre : genres) {
            final List<GenreGameLinkEntity> genreLinks = genreGameLinkDao.getByGenreId(genre.getId());
            if (genreLinks.isEmpty()) {
                LOG.debug("Genre={} was only linked to gameId={}, deleting...", genre, gameId);
                genresToDelete.add(genre);
            }
        }
        if (!genresToDelete.isEmpty()) {
            genreDao.deleteIds(ListUtils.map(genresToDelete, GenreEntity::getId));
        }

        libraryGameLinkDao.deleteByGameId(gameId);
    }

    @Override
    @SneakyThrows
    public List<Game> getAllGames() {
        LOG.debug("Fetching all games...");
        final List<GameEntity> games = gameDao.queryForAll();

        // This is required due to lack of support for intelligent joins in OrmLite... :/
        final GenreExtractor genreExtractor = createGenreExtractor();
        final LibraryExtractor libraryExtractor = createLibraryExtractor();
        return ListUtils.map(games, game -> translateGame(game, genreExtractor, libraryExtractor));
    }

    private GenreExtractor createGenreExtractor() throws SQLException {
        final List<Genre> genres = getAllGenres();
        final Map<Id<Genre>, Genre> genreMap = ListUtils.toMap(genres, Genre::getId);

        final List<GenreGameLinkEntity> genreGames = genreGameLinkDao.queryForAll();
        final Map<Integer, List<GenreGameLinkEntity>> genreGameMap = ListUtils.toMultiMap(genreGames, entity -> entity.getGame().getId());

        return new GenreExtractor(genreMap, genreGameMap);
    }

    private LibraryExtractor createLibraryExtractor() throws SQLException {
        final List<Library> libraries = getAllLibraries();
        final Map<Id<Library>, Library> libraryMap = ListUtils.toMap(libraries, Library::getId);

        final List<LibraryGameLinkEntity> libraryGames = libraryGameLinkDao.queryForAll();
        final Map<Integer, List<LibraryGameLinkEntity>> libraryGameMap = ListUtils.toMultiMap(libraryGames, entity -> entity.getGame().getId());

        return new LibraryExtractor(libraryMap, libraryGameMap);
    }

    private Game translateGame(GameEntity entity, GenreExtractor genreExtractor, LibraryExtractor libraryExtractor) {
        final List<Genre> genres = genreExtractor.getData(entity.getId());
        final List<Library> libraries = libraryExtractor.getData(entity.getId());
        return gameTranslator.translate(entity, genres, libraries);
    }

    @Override
    @SneakyThrows
    public Game getGameById(Id<Game> id) {
        final GameEntity entity = gameDao.queryForId(id.getId());
        if (entity == null) {
            throw new DataException("Couldn't find game with id: %d", id);
        }
        final List<Genre> genres = getGenresByGame(entity);
        final List<Library> libraries = getLibrariesByGame(entity);
        return gameTranslator.translate(entity, genres, libraries);
    }

    private List<Genre> getGenresByGame(GameEntity game) throws SQLException {
        final List<GenreEntity> entities = genreGameLinkDao.getGenresByGameId(game.getId());
        return translateGenres(entities);
    }

    private List<Library> getLibrariesByGame(GameEntity entity) throws SQLException {
        final List<LibraryEntity> entities = libraryGameLinkDao.getLibrariesByGameId(entity.getId());
        return translateLibraries(entities);
    }

    @Override
    @SneakyThrows
    public boolean hasGameForPath(Path path) {
        return gameDao.queryByPath(path) != null;
    }

    @Override
    @SneakyThrows
    public Library addLibrary(String name, Path path, GamePlatform platform) {
        final LibraryEntity entity = new LibraryEntity();
        entity.setName(name);
        entity.setPath(path.toString());
        entity.setPlatform(platform);

        LOG.debug("Inserting {}...", entity);
        if (libraryDao.create(entity) != 1) {
            throw new DataException("Error inserting library: %s", entity);
        }
        return translateLibrary(entity);
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
    public List<Library> getAllLibraries() {
        LOG.debug("Fetching all libraries...");
        final List<LibraryEntity> entities = libraryDao.queryForAll();
        return translateLibraries(entities);
    }

    @Override
    @SneakyThrows
    public Library getLibraryById(Id<Library> id) {
        final LibraryEntity entity = libraryDao.queryForId(id.getId());
        if (entity == null) {
            throw new DataException("Couldn't find library with id: %d", id);
        }
        return translateLibrary(entity);
    }

    @Override
    @SneakyThrows
    public boolean hasLibraryForPath(Path path) {
        return libraryDao.queryByPath(path)  != null;
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
    public List<Genre> getAllGenres() {
        LOG.debug("Fetching all genres...");
        final List<GenreEntity> entities = genreDao.queryForAll();
        return translateGenres(entities);
    }

    @Override
    @SneakyThrows
    public List<ExcludedPath> getAllExcludedPaths() {
        final List<ExcludedPathEntity> entities = excludedPathDao.queryForAll();
        return ListUtils.map(entities, excludedPathTranslator::translate);
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

    private List<Genre> translateGenres(List<GenreEntity> entities) {
        return ListUtils.map(entities, this::translateGenre);
    }

    private Genre translateGenre(GenreEntity entity) {
        return genreTranslator.translate(entity);
    }

    private List<Library> translateLibraries(List<LibraryEntity> entities) {
        return ListUtils.map(entities, this::translateLibrary);
    }

    private Library translateLibrary(LibraryEntity entity) {
        return libraryTranslator.translate(entity);
    }
}
