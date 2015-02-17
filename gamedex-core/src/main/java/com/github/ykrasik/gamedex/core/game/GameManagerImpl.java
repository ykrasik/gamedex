package com.github.ykrasik.gamedex.core.game;

import com.github.ykrasik.gamedex.common.exception.GameDexException;
import com.github.ykrasik.gamedex.common.service.AbstractService;
import com.github.ykrasik.gamedex.common.util.ListUtils;
import com.github.ykrasik.gamedex.common.util.PlatformUtils;
import com.github.ykrasik.gamedex.datamodel.GamePlatform;
import com.github.ykrasik.gamedex.datamodel.persistence.Game;
import com.github.ykrasik.gamedex.datamodel.persistence.Genre;
import com.github.ykrasik.gamedex.datamodel.persistence.Id;
import com.github.ykrasik.gamedex.datamodel.persistence.Library;
import com.github.ykrasik.gamedex.datamodel.provider.UnifiedGameInfo;
import com.github.ykrasik.gamedex.persistence.PersistenceService;
import com.gs.collections.impl.map.mutable.MutableMapFactoryImpl;
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
import java.util.Map;
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

    private final Map<FilterType, Predicate<Game>> filters = new MutableMapFactoryImpl().of(
        FilterType.NAME, NO_FILTER,
        FilterType.GENRE, NO_FILTER,
        FilterType.LIBRARY, NO_FILTER
    );

    private GameSort sort = GameSort.NAME_ASC;

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
        final List<Genre> genres = persistenceService.getAllGenres();
        return FXCollections.observableArrayList(genres);
    }

    @Override
    public ReadOnlyListProperty<Game> gamesProperty() {
        return gamesProperty;
    }

    @Override
    public void nameFilter(String name) {
        setFilter(FilterType.NAME, game -> StringUtils.containsIgnoreCase(game.getName(), name));
    }

    @Override
    public void noNameFilter() {
        clearFilter(FilterType.NAME);
    }

    @Override
    public void genreFilter(List<Genre> genres) {
        setFilter(FilterType.GENRE, game -> ListUtils.containsAny(game.getGenres(), genres));
    }

    @Override
    public void noGenreFilter() {
        clearFilter(FilterType.GENRE);
    }

    @Override
    public void libraryFilter(Library library) {
        setFilter(FilterType.LIBRARY, game -> game.getLibraries().contains(library));
    }

    @Override
    public void noLibraryFilter() {
        clearFilter(FilterType.LIBRARY);
    }

    private void clearFilter(FilterType filterType) {
        setFilter(filterType, NO_FILTER);
    }

    private void setFilter(FilterType filterType, Predicate<Game> filter) {
        filters.put(filterType, filter);
        refreshGames();
    }

    @Override
    public void sort(GameSort sort) {
        this.sort = sort;
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
        Predicate<Game> mergedFilter = NO_FILTER;
        boolean noFilter = true;
        for (Predicate<Game> filter : filters.values()) {
            if (filter != NO_FILTER) {
                noFilter = false;
                mergedFilter = mergedFilter.and(filter);
            }
        }

        if (noFilter) {
            return NO_FILTER;
        } else {
            return mergedFilter;
        }
    }

    private Comparator<Game> getComparator(GameSort sort) {
        switch (sort) {
            case NAME_ASC: return GameComparators.nameAsc();
            case NAME_DESC: return GameComparators.nameDesc();

            case CRITIC_SCORE_ASC: return GameComparators.criticScoreAsc();
            case CRITIC_SCORE_DESC: return GameComparators.criticScoreDesc();

            case USER_SCORE_ASC: return GameComparators.userScoreAsc();
            case USER_SCORE_DESC: return GameComparators.userScoreDesc();

            case RELEASE_DATE_ASC: return GameComparators.releaseDateAsc();
            case RELEASE_DATE_DESC: return GameComparators.releaseDateDesc();

            case DATE_ADDED_ASC: return GameComparators.lastModifiedAsc();
            case DATE_ADDED_DESC: return GameComparators.lastModifiedDesc();

            default: throw new GameDexException("Invalid sort value: %s", sort);
        }
    }

    private enum FilterType {
        NAME,
        GENRE,
        LIBRARY
    }
}
