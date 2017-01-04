package com.github.ykrasik.gamedex.core.manager.provider;

import com.github.ykrasik.gamedex.core.service.action.ExcludeException;
import com.github.ykrasik.gamedex.core.service.action.SkipException;
import com.github.ykrasik.gamedex.core.service.config.ConfigService;
import com.github.ykrasik.gamedex.core.service.screen.search.GameSearchChoice;
import com.github.ykrasik.gamedex.core.service.screen.search.GameSearchScreen;
import com.github.ykrasik.yava.option.Opt;
import com.gitlab.ykrasik.gamedex.provider.DataProvider;
import com.gitlab.ykrasik.gamedex.provider.ProviderSearchResult;
import com.gs.collections.api.list.ImmutableList;
import javafx.beans.property.*;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;

/**
 * @author Yevgeny Krasik
 */
@Slf4j
@Accessors(fluent = true)
@RequiredArgsConstructor
public class GameInfoProviderManagerImpl implements GameInfoProviderManager {
    @Getter private final StringProperty messageProperty = new SimpleStringProperty();
    private final BooleanProperty fetchingProperty = new SimpleBooleanProperty();

    @NonNull private final ConfigService configService;
    @NonNull private final GameSearchScreen gameSearchScreen;
    @NonNull private final DataProvider gameInfoProvider;

    @Override
    public ReadOnlyBooleanProperty fetchingProperty() {
        return fetchingProperty;
    }

    @Override
    public Opt<GameInfo> fetchGameInfo(String name, SearchContext context) throws Exception {
        return doFetchGameInfo(name.trim(), context);
    }

    private Opt<GameInfo> doFetchGameInfo(String name, SearchContext context) throws Exception {
        assertNotStopped();
        final ImmutableList<ProviderSearchResult> searchResults = searchGames(name, context);
        assertNotStopped();

        if (searchResults.size() == 1) {
            return Opt.some(fetchGameInfoFromSearchResult(searchResults.get(0)));
        }

        assertNotAutoSkip();
        final GameSearchChoice choice = gameSearchScreen.show(name, context.path(), gameInfoProvider.getInfo(), searchResults);
        switch (choice.type()) {
            case SELECT_RESULT:
                final ProviderSearchResult selectedResult = choice.searchResult().get();
                log.info("Result selected: {}", selectedResult);

                // Add all search results except the selected one to excluded list.
                final ImmutableList<ProviderSearchResult> resultsToExclude = searchResults.newWithout(selectedResult);
                context.addExcludedNames(resultsToExclude.collect(ProviderSearchResult::getName));
                return Opt.some(fetchGameInfoFromSearchResult(selectedResult));

            case NEW_NAME:
                log.info("New name requested: {}", choice.newName().get());

                // Add all current search results to excluded list.
                context.addExcludedNames(searchResults.collect(ProviderSearchResult::getName));
                return doFetchGameInfo(choice.newName().get(), context);

            case SKIP: log.info("Skip selected."); throw new SkipException();
            case EXCLUDE: log.info("Exclude selected."); throw new ExcludeException();
            case PROCEED_ANYWAY: log.info("Proceed anyway selected."); return Opt.none();
            default: throw new IllegalStateException("Invalid choice type: " + choice.type());
        }
    }

    private ImmutableList<ProviderSearchResult> searchGames(String name, SearchContext context) throws Exception {
        message("Searching '%s'...", name);
        fetchingProperty.set(true);
        final ImmutableList<ProviderSearchResult> searchResults;
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
        final ImmutableList<ProviderSearchResult> filteredSearchResults = searchResults.select(result -> !excludedNames.contains(result.getName()));
        if (!filteredSearchResults.isEmpty()) {
            message("%d remaining results.", filteredSearchResults.size());
            return filteredSearchResults;
        } else {
            message("No search results after filtering, reverting...");
            return searchResults;
        }
    }

    private GameInfo fetchGameInfoFromSearchResult(ProviderSearchResult providerSearchResult) throws Exception {
        fetchingProperty.set(true);
        try {
            message("Fetching '%s'...", providerSearchResult.getName());
            final GameInfo gameInfo = gameInfoProvider.fetch(providerSearchResult);
            message("Done.");
            return gameInfo;
        } finally {
            fetchingProperty.set(false);
        }
    }

    private void assertNotAutoSkip() {
        if (configService.isAutoSkip()) {
            message("AutoSkip is on.");
            throw new SkipException();
        }
    }

    @Override
    public void message(String message) {
        final String messageWithProvider = String.format("%s: %s", gameInfoProvider.getInfo().getName(), message);
        messageProperty.set(messageWithProvider);
    }
}
