package com.github.ykrasik.indexter.games.manager.game;

import com.github.ykrasik.indexter.AbstractService;
import com.github.ykrasik.indexter.exception.IndexterException;
import com.github.ykrasik.indexter.games.datamodel.GamePlatform;
import com.github.ykrasik.indexter.games.datamodel.info.GameInfo;
import com.github.ykrasik.indexter.games.datamodel.persistence.Game;
import com.github.ykrasik.indexter.games.persistence.PersistenceService;
import com.github.ykrasik.indexter.id.Id;
import com.github.ykrasik.indexter.util.PlatformUtils;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.function.Predicate;

/**
 * @author Yevgeny Krasik
 */
public class GameManagerImpl extends AbstractService implements GameManager {
    private final PersistenceService persistenceService;

    private ObservableList<Game> games = FXCollections.emptyObservableList();
    private ObjectProperty<ObservableList<Game>> itemsProperty = new SimpleObjectProperty<>();

    public GameManagerImpl(PersistenceService persistenceService) {
        this.persistenceService = persistenceService;
    }

    @Override
    protected void doStart() throws Exception {
        this.games = FXCollections.observableArrayList(persistenceService.getAllGames());
        this.itemsProperty = new SimpleObjectProperty<>(games);
    }

    @Override
    protected void doStop() throws Exception {

    }

    @Override
    public Game addGame(GameInfo gameInfo, Path path, GamePlatform platform) {
        final Game game = persistenceService.addGame(gameInfo, path, platform);

        // Update cache.
        PlatformUtils.runLater(() -> games.add(game));

        return game;
    }

    @Override
    public void deleteGame(Game game) {
        LOG.info("Deleting game: {}...", game);

        // Delete from db.
        persistenceService.deleteGame(game.getId());

        // Delete from cache.
        PlatformUtils.runLater(() -> {
            games.remove(game);
            refreshItemsProperty();
        });
    }

    @Override
    public Game getGameById(Id<Game> id) {
        return persistenceService.getGameById(id);
    }

    @Override
    public boolean isPathMapped(Path path) {
        return persistenceService.hasGameForPath(path);
    }

    @Override
    public ObservableList<Game> getAllGames() {
        return games;
    }

    @Override
    public void sort(GameSort sort) {
        final Comparator<Game> comparator = getComparator(sort);
        PlatformUtils.runLater(() -> {
            FXCollections.sort(games, comparator);
            refreshItemsProperty();
        });
    }

    @Override
    public void filter(Predicate<Game> filter) {
        final ObservableList<Game> filteredGames = games.filtered(filter);
        PlatformUtils.runLater(() -> itemsProperty.setValue(filteredGames));
    }

    @Override
    public void unFilter() {
        PlatformUtils.runLater(this::refreshItemsProperty);
    }

    private Comparator<Game> getComparator(GameSort sort) {
        switch (sort) {
            case DATE_ADDED: return GameComparators.dateAddedComparator();
            case NAME: return GameComparators.nameComparator();
            case CRITIC_SCORE: return GameComparators.criticScoreComparator();
            case USER_SCORE: return GameComparators.userScoreComparator();
            case RELEASE_DATE: return GameComparators.releaseDateComparator();
            default: throw new IndexterException("Invalid sort value: %s", sort);
        }
    }

    @Override
    public ReadOnlyObjectProperty<ObservableList<Game>> itemsProperty() {
        return itemsProperty;
    }

    // This is a hack... it seems that the gridView that is bound to this doesn't know how to refresh itself properly.
    private void refreshItemsProperty() {
        itemsProperty.setValue(FXCollections.emptyObservableList());
        itemsProperty.setValue(games);
    }
}
