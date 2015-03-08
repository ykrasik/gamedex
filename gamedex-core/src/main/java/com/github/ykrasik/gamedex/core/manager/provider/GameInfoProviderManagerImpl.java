package com.github.ykrasik.gamedex.core.manager.provider;

import com.github.ykrasik.gamedex.common.exception.GameDexException;
import com.github.ykrasik.gamedex.core.service.action.ExcludeException;
import com.github.ykrasik.gamedex.core.service.action.SkipException;
import com.github.ykrasik.gamedex.core.service.config.ConfigService;
import com.github.ykrasik.gamedex.core.service.config.ConfigType;
import com.github.ykrasik.gamedex.core.service.screen.search.GameSearchChoice;
import com.github.ykrasik.gamedex.core.service.screen.search.GameSearchScreen;
import com.github.ykrasik.gamedex.datamodel.provider.GameInfo;
import com.github.ykrasik.gamedex.datamodel.provider.SearchResult;
import com.github.ykrasik.gamedex.provider.GameInfoProvider;
import com.github.ykrasik.opt.Opt;
import com.gs.collections.api.list.ImmutableList;
import javafx.beans.property.*;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

import java.util.Collection;

/**
 * @author Yevgeny Krasik
 */
@Accessors(fluent = true)
@RequiredArgsConstructor
public class GameInfoProviderManagerImpl implements GameInfoProviderManager {
    private final StringProperty messageProperty = new SimpleStringProperty();
    private final BooleanProperty fetchingProperty = new SimpleBooleanProperty();

    @NonNull private final ConfigService configService;
    @NonNull private final GameSearchScreen gameSearchScreen;
    @NonNull private final GameInfoProvider gameInfoProvider;

    @Override
    public ReadOnlyStringProperty messageProperty() {
        return messageProperty;
    }

    @Override
    public ReadOnlyBooleanProperty fetchingProperty() {
        return fetchingProperty;
    }

    @Override
    public Opt<GameInfo> fetchGameInfo(String name, SearchContext context) throws Exception {
        try {
            return doFetchGameInfo(name.trim(), context);
        } finally {
            messageProperty.set(null);
        }
    }

    private Opt<GameInfo> doFetchGameInfo(String name, SearchContext context) throws Exception {
        checkStopped();
        final ImmutableList<SearchResult> searchResults = searchGames(name, context);
        checkStopped();

        if (searchResults.size() == 1) {
            return Opt.of(fetchGameInfoFromSearchResult(searchResults.get(0)));
        }

        assertNotAutoSkip();
        final GameSearchChoice choice = gameSearchScreen.show(gameInfoProvider, name, context, searchResults);
        switch (choice.getType()) {
            case SKIP: throw new SkipException();
            case EXCLUDE: throw new ExcludeException();
            case PROCEED_ANYWAY: return Opt.absent();
            case OK: return Opt.of(fetchGameInfoFromSearchResult(choice.getSearchResult().get()));
            default: throw new IllegalStateException("Invalid choice type: " + choice.getType());
        }
    }

    private ImmutableList<SearchResult> searchGames(String name, SearchContext context) throws Exception {
        message("Searching '%s'...", name);
        fetchingProperty.set(true);
        final ImmutableList<SearchResult> searchResults;
        try {
            searchResults = gameInfoProvider.search(name, context.platform());
        } finally {
            fetchingProperty.set(false);
        }
        message("Found %d results for '%s'.", searchResults.size(), name);

        final Collection<String> excludedNames = context.getExcludedNames();
        if (searchResults.size() <= 1 || excludedNames.isEmpty()) {
            return searchResults;
        }

        message("Filtering previously encountered search results...");
        final ImmutableList<SearchResult> filteredSearchResults = searchResults.select(result -> !excludedNames.contains(result.getName()));
        if (!filteredSearchResults.isEmpty()) {
            message("%d remaining results.", filteredSearchResults.size());
            return filteredSearchResults;
        } else {
            message("No search results after filtering, reverting...");
            return searchResults;
        }
    }

    private GameInfo fetchGameInfoFromSearchResult(SearchResult searchResult) throws Exception {
        fetchingProperty.set(true);
        try {
            message("Fetching '%s'...", searchResult.getName());
            final GameInfo gameInfo = gameInfoProvider.fetch(searchResult);
            message("Done.");
            return gameInfo;
        } finally {
            fetchingProperty.set(false);
        }
    }

    private void assertNotAutoSkip() {
        if (isAutoSkip()) {
            message("AutoSkip is on.");
            throw new SkipException();
        }
    }

    private boolean isAutoSkip() {
        return configService.<Boolean>property(ConfigType.AUTO_SKIP).get();
    }

    private void message(String format, Object... args) {
        message(String.format(format, args));
    }

    private void message(String message) {
        final String messageWithProvider = gameInfoProvider.getName() + ": " + message;
        messageProperty.set(messageWithProvider);
    }

    private void checkStopped() {
        if (Thread.interrupted()) {
            message("Stopping...");
            throw new GameDexException("Stopped.");
        }
    }
}
