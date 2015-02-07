package com.github.ykrasik.indexter.games.persistence;

import com.github.ykrasik.indexter.AbstractService;
import com.github.ykrasik.indexter.exception.DataException;
import com.github.ykrasik.indexter.games.datamodel.GamePlatform;
import com.github.ykrasik.indexter.games.datamodel.info.GameInfo2;
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
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import java.nio.file.Path;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Yevgeny Krasik
 */
@RequiredArgsConstructor
public class PersistenceServiceImpl extends AbstractService implements PersistenceService {
    @NonNull private final PersistenceProperties properties;
    @NonNull private final GameEntityTranslator gameTranslator;
    @NonNull private final GenreEntityTranslator genreTranslator;
    @NonNull private final LibraryEntityTranslator libraryTranslator;

    private JdbcPooledConnectionSource connectionSource;
    private GameDao gameDao;
    private GenreDao genreDao;
    private GenreGameLinkDao genreGameLinkDao;
    private LibraryDao libraryDao;
    private LibraryGameLinkDao libraryGameLinkDao;

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
    public Game addGame(GameInfo2 gameInfo, Path path, GamePlatform platform) {
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
        return gameDao.queryByPath(path) != null;
    }

    private Game translateGame(GameEntity entity) {
        final List<Genre> genres = getGenresByGame(entity);
        return gameTranslator.translate(entity, genres);
    }

    @SneakyThrows
    private List<Genre> getGenresByGame(GameEntity game) {
        final List<GenreEntity> genreEntities = genreGameLinkDao.getGenresByGameId(game.getId());
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
    public boolean hasLibraryForPath(Path path) {
        return libraryDao.queryByPath(path)  != null;
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

    @Override
    @SneakyThrows
    public List<Genre> getAllGenres() {
        final List<GenreEntity> entities = genreDao.queryForAll();
        return translateGenres(entities);
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
