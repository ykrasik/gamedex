package com.github.ykrasik.gamedex.core.game;

import com.github.ykrasik.gamedex.common.exception.GameDexException;
import com.github.ykrasik.gamedex.common.service.AbstractService;
import com.github.ykrasik.gamedex.common.util.ListUtils;
import com.github.ykrasik.gamedex.common.util.PlatformUtils;
import com.github.ykrasik.gamedex.datamodel.GamePlatform;
import com.github.ykrasik.gamedex.datamodel.persistence.Game;
import com.github.ykrasik.gamedex.datamodel.persistence.Genre;
import com.github.ykrasik.gamedex.datamodel.persistence.Id;
import com.github.ykrasik.gamedex.datamodel.provider.UnifiedGameInfo;
import com.github.ykrasik.gamedex.persistence.PersistenceService;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ReadOnlyListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

/**
 * @author Yevgeny Krasik
 */
@RequiredArgsConstructor
public class GameManagerImpl extends AbstractService implements GameManager {
    private static final Predicate<Game> NO_FILTER = any -> true;

    @NonNull private final PersistenceService persistenceService;

    private final ListProperty<Game> gamesProperty = new SimpleListProperty<>();
    private ObservableList<Game> games = FXCollections.emptyObservableList();

    private GameSort sort = GameSort.NAME;
    private Predicate<Game> nameFilter = NO_FILTER;
    private Predicate<Game> genreFilter = NO_FILTER;

    // FIXME: This is too slow. Need to find a way to stream the images from the db.
    @Override
    protected void doStart() throws Exception {
        LOG.info("Loading games...");
        games = FXCollections.observableArrayList(persistenceService.getAllGames());
        gamesProperty.set(games);
        doRefreshGames();
        LOG.info("Games: {}", games.size());
    }

    @Override
    protected void doStop() throws Exception {

    }

    @Override
    public Game addGame(UnifiedGameInfo gameInfo, Path path, GamePlatform platform) {
        final Game game = persistenceService.addGame(gameInfo, path, platform);
        LOG.info("Added game: {}", game);

        // Update cache.
        PlatformUtils.runLaterIfNecessary(() -> {
            games.add(game);
            doRefreshGames();
        });
        return game;
    }

    @Override
    public void deleteGame(Game game) {
        persistenceService.deleteGame(game.getId());
        LOG.info("Deleted game: {}...", game);

        // Delete from cache.
        PlatformUtils.runLaterIfNecessary(() -> {
            games.remove(game);
            doRefreshGames();
        });
    }

    @Override
    public void deleteGames(Collection<Game> games) {
        for (Game game : games) {
            persistenceService.deleteGame(game.getId());
        }

        // Delete from cache.
        PlatformUtils.runLaterIfNecessary(() -> {
            this.games.removeAll(games);
            doRefreshGames();
        });
    }

    @Override
    public ObservableList<Game> getAllGames() {
        return games;
    }

    @Override
    public Game getGameById(Id<Game> id) {
        return persistenceService.getGameById(id);
    }

    @Override
    public boolean isGame(Path path) {
        return persistenceService.hasGameForPath(path);
    }

    @Override
    public ObservableList<Genre> getAllGenres() {
        final ObservableList<Genre> genres = FXCollections.observableArrayList(persistenceService.getAllGenres());
        FXCollections.sort(genres);
        return genres;
    }

    @Override
    public ReadOnlyListProperty<Game> gamesProperty() {
        return gamesProperty;
    }

    @Override
    public void sort(GameSort sort) {
        this.sort = sort;
        refreshGames();
    }

    @Override
    public void nameFilter(String name) {
        nameFilter = game -> StringUtils.containsIgnoreCase(game.getName(), name);
        refreshGames();
    }

    @Override
    public void noNameFilter() {
        nameFilter = NO_FILTER;
        refreshGames();
    }

    @Override
    public void genreFilter(List<Genre> genres) {
        genreFilter = game -> ListUtils.containsAny(game.getGenres(), genres);
        refreshGames();
    }

    @Override
    public void noGenreFilter() {
        genreFilter = NO_FILTER;
        refreshGames();
    }

    private void refreshGames() {
        PlatformUtils.runLaterIfNecessary(this::doRefreshGames);
    }

    private void doRefreshGames() {
        // Filter
        final Predicate<Game> filter = mergeFilters();
        final ObservableList<Game> filtered = (filter != NO_FILTER) ? FXCollections.observableArrayList(games.filtered(filter)) : games;

        // Sort
        FXCollections.sort(filtered, getComparator(sort));

        // It seems there is a bug with GridView - the list doesn't update when deleting unless the reference is re-set.
        gamesProperty.setValue(FXCollections.emptyObservableList());
        gamesProperty.setValue(filtered);
    }

    private Predicate<Game> mergeFilters() {
        if (nameFilter == NO_FILTER && genreFilter == NO_FILTER) {
            return NO_FILTER;
        } else {
           return nameFilter.and(genreFilter);
        }
    }

    private Comparator<Game> getComparator(GameSort sort) {
        switch (sort) {
            case DATE_ADDED: return GameComparators.dateAddedComparator();
            case NAME: return GameComparators.nameComparator();
            case CRITIC_SCORE: return GameComparators.criticScoreComparator();
            case USER_SCORE: return GameComparators.userScoreComparator();
            case RELEASE_DATE: return GameComparators.releaseDateComparator();
            default: throw new GameDexException("Invalid sort value: %s", sort);
        }
    }
}
