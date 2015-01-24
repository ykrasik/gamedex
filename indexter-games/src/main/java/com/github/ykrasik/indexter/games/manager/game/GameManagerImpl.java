package com.github.ykrasik.indexter.games.manager.game;

import com.github.ykrasik.indexter.AbstractService;
import com.github.ykrasik.indexter.exception.IndexterException;
import com.github.ykrasik.indexter.games.datamodel.Game;
import com.github.ykrasik.indexter.games.datamodel.GameSort;
import com.github.ykrasik.indexter.games.datamodel.LocalGame;
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
import java.util.Optional;

/**
 * @author Yevgeny Krasik
 */
public class GameManagerImpl extends AbstractService implements GameManager {
    private final PersistenceService persistenceService;

    private ObservableList<LocalGame> games = FXCollections.emptyObservableList();
    private ObjectProperty<ObservableList<LocalGame>> itemsProperty = new SimpleObjectProperty<>();

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
    public LocalGame addGame(Game game, Path path) {
        final LocalGame localGame = persistenceService.addGame(game, path);

        // Update cache.
        PlatformUtils.runLater(() -> games.add(localGame));

        return localGame;
    }

    @Override
    public void deleteGame(LocalGame game) {
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
    public LocalGame getGameById(Id<LocalGame> id) {
        return persistenceService.getGameById(id);
    }

    @Override
    public Optional<LocalGame> getGameByPath(Path path) {
        return persistenceService.getGameByPath(path);
    }

    @Override
    public boolean isGameMapped(Path path) {
        return getGameByPath(path).isPresent();
    }

    @Override
    public ObservableList<LocalGame> getAllGames() {
        return games;
    }

    @Override
    public void sort(GameSort sort) {
        final Comparator<LocalGame> comparator = getComparator(sort);
        PlatformUtils.runLater(() -> {
            FXCollections.sort(games, comparator);
            refreshItemsProperty();
        });
    }

    private Comparator<LocalGame> getComparator(GameSort sort) {
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
    public ReadOnlyObjectProperty<ObservableList<LocalGame>> itemsProperty() {
        return itemsProperty;
    }

    // This is a hack... it seems that the gridView that is bound to this doesn't know how to refresh itself properly.
    private void refreshItemsProperty() {
        itemsProperty.setValue(FXCollections.emptyObservableList());
        itemsProperty.setValue(games);
    }
}
