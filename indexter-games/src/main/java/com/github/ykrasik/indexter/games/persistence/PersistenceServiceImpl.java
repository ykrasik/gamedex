package com.github.ykrasik.indexter.games.persistence;

import com.github.ykrasik.indexter.AbstractService;
import com.github.ykrasik.indexter.exception.DataException;
import com.github.ykrasik.indexter.games.datamodel.GamePlatform;
import com.github.ykrasik.indexter.games.datamodel.info.GameInfo;
import com.github.ykrasik.indexter.games.datamodel.persistence.Game;
import com.github.ykrasik.indexter.games.datamodel.persistence.Genre;
import com.github.ykrasik.indexter.games.datamodel.persistence.Library;
import com.github.ykrasik.indexter.games.persistence.config.PersistenceProperties;
import com.github.ykrasik.indexter.games.persistence.dao.*;
import com.github.ykrasik.indexter.games.persistence.entity.*;
import com.github.ykrasik.indexter.games.persistence.translator.GameEntityTranslator;
import com.github.ykrasik.indexter.games.persistence.translator.GenreEntityTranslator;
import com.github.ykrasik.indexter.games.persistence.translator.LibraryEntityTranslator;
import com.github.ykrasik.indexter.id.Id;
import com.github.ykrasik.indexter.util.DateUtils;
import com.github.ykrasik.indexter.util.ListUtils;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcPooledConnectionSource;
import com.j256.ormlite.table.TableUtils;
import lombok.SneakyThrows;

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
    private final GenreEntityTranslator genreTranslator;
    private final LibraryEntityTranslator libraryTranslator;

    private JdbcPooledConnectionSource connectionSource;
    private GameDao gameDao;
    private GenreDao genreDao;
    private GenreGameLinkDao genreGameLinkDao;
    private LibraryDao libraryDao;
    private LibraryGameLinkDao libraryGameLinkDao;

    public PersistenceServiceImpl(PersistenceProperties properties,
                                  GameEntityTranslator gameTranslator,
                                  GenreEntityTranslator genreTranslator,
                                  LibraryEntityTranslator libraryTranslator) {
        this.properties = Objects.requireNonNull(properties);
        this.gameTranslator = Objects.requireNonNull(gameTranslator);
        this.genreTranslator = Objects.requireNonNull(genreTranslator);
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
        TableUtils.createTableIfNotExists(connectionSource, GenreEntity.class);
        TableUtils.createTableIfNotExists(connectionSource, GenreGameLinkEntity.class);
        TableUtils.createTableIfNotExists(connectionSource, LibraryEntity.class);
        TableUtils.createTableIfNotExists(connectionSource, LibraryGameLinkEntity.class);
    }

    @SuppressWarnings("unchecked")
    private void instantiateDaos() throws SQLException {
        gameDao = DaoManager.createDao(connectionSource, GameEntity.class);
        genreDao = DaoManager.createDao(connectionSource, GenreEntity.class);
        genreGameLinkDao = DaoManager.createDao(connectionSource, GenreGameLinkEntity.class);
        ((GenreGameLinkDaoImpl)genreGameLinkDao).setDaos(gameDao, genreDao);
        libraryDao = DaoManager.createDao(connectionSource, LibraryEntity.class);
        libraryGameLinkDao = DaoManager.createDao(connectionSource, LibraryGameLinkEntity.class);
    }

    @Override
    protected void doStop() throws Exception {
        connectionSource.close();
    }

    @Override
    @SneakyThrows
    public Game addGame(GameInfo gameInfo, Path path, GamePlatform platform) {
        // Insert game.
        final GameEntity game = gameTranslator.translate(gameInfo);
        game.setPath(path.toString());
        game.setPlatform(platform);
        LOG.debug("Inserting {}...", game);
        if (gameDao.create(game) != 1) {
            throw new DataException("Error inserting game: %s", gameInfo);
        }

        // Insert all new genres.
        final List<GenreEntity> genresEntities = ListUtils.map(gameInfo.getGenres(), genreDao::getOrCreateByName);

        // Link genres to game.
        for (GenreEntity genre : genresEntities) {
            final GenreGameLinkEntity genreGameLinkEntity = new GenreGameLinkEntity();
            genreGameLinkEntity.setGame(game);
            genreGameLinkEntity.setGenre(genre);
            genreGameLinkDao.create(genreGameLinkEntity);
        }

        final List<Genre> genres = translateGenres(genresEntities);
        return Game.from(new Id<>(game.getId()), path, platform, DateUtils.toLocalDateTime(game.getLastModified()), gameInfo, genres);
    }

    @Override
    @SneakyThrows
    public void deleteGame(Id<Game> id) {
        final int gameId = id.getId();
        LOG.debug("Deleting game with id {}...", gameId);
        if (gameDao.deleteById(gameId) != 1) {
            throw new DataException("Error deleting game: %d. Entry doesn't exist?", gameId);
        }
        genreGameLinkDao.deleteByGameId(gameId);
        libraryGameLinkDao.deleteByGameId(gameId);
    }

    @Override
    @SneakyThrows
    public List<Game> getAllGames() {
        LOG.debug("Fetching all games...");
        final List<GameEntity> games = gameDao.queryForAll();
        return ListUtils.map(games, this::translateGame);
    }

    @Override
    @SneakyThrows
    public Game getGameById(Id<Game> id) {
        final GameEntity entity = gameDao.queryForId(id.getId());
        if (entity == null) {
            throw new DataException("Couldn't find game with id: %d", id);
        }
        return translateGame(entity);
    }

    @Override
    @SneakyThrows
    public boolean hasGameForPath(Path path) {
        return Optional.ofNullable(gameDao.queryByPath(path)).isPresent();
    }

    private Game translateGame(GameEntity entity) {
        final List<Genre> genres = getGenresByGame(entity);
        return gameTranslator.translate(entity, genres);
    }

    @SneakyThrows
    private List<Genre> getGenresByGame(GameEntity game) {
        final List<GenreEntity> genreEntities = genreGameLinkDao.getGenresByGame(game);
        return translateGenres(genreEntities);
    }

    @Override
    @SneakyThrows
    public Library addLibrary(String name, Path path, GamePlatform platform) {
        final LibraryEntity library = new LibraryEntity();
        library.setName(name);
        library.setPath(path.toString());
        library.setPlatform(platform);

        LOG.debug("Inserting {}...", library);
        if (libraryDao.create(library) != 1) {
            throw new DataException("Error inserting library: %s", library);
        }

        return new Library(new Id<>(library.getId()), name, path, platform);
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
    public Optional<Library> getLibraryByPath(Path path) {
        final LibraryEntity library = libraryDao.queryByPath(path);
        return Optional.ofNullable(library).map(this::translateLibrary);
    }

    @Override
    @SneakyThrows
    public void addGameToLibrary(Game game, Library library) {
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
