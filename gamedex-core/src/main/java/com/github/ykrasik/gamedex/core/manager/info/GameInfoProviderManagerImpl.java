package com.github.ykrasik.gamedex.core.manager.info;

import com.github.ykrasik.gamedex.common.exception.GameDexException;
import com.github.ykrasik.gamedex.core.config.ConfigService;
import com.github.ykrasik.gamedex.core.config.ConfigType;
import com.github.ykrasik.gamedex.core.service.action.SkipException;
import com.github.ykrasik.gamedex.core.service.dialog.DialogService;
import com.github.ykrasik.gamedex.core.service.dialog.MultipleSearchResultsDialogParams;
import com.github.ykrasik.gamedex.core.service.dialog.NoSearchResultsDialogParams;
import com.github.ykrasik.gamedex.core.service.dialog.choice.DefaultDialogChoiceResolver;
import com.github.ykrasik.gamedex.core.service.dialog.choice.DialogChoice;
import com.github.ykrasik.gamedex.datamodel.GamePlatform;
import com.github.ykrasik.gamedex.datamodel.provider.GameInfo;
import com.github.ykrasik.gamedex.datamodel.provider.SearchResult;
import com.github.ykrasik.gamedex.provider.GameInfoProvider;
import com.github.ykrasik.opt.Opt;
import com.gs.collections.api.list.ImmutableList;
import javafx.beans.property.*;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

import java.nio.file.Path;
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
    @NonNull private final DialogService dialogService;
    @NonNull private final GameInfoProvider gameInfoProvider;
    private final boolean canProceedWithout;

    @Override
    public ReadOnlyStringProperty messageProperty() {
        return messageProperty;
    }

    @Override
    public ReadOnlyBooleanProperty fetchingProperty() {
        return fetchingProperty;
    }

    @Override
    public Opt<GameInfo> fetchGameInfo(String name, Path path, GamePlatform platform, SearchContext context) throws Exception {
        try {
            return doFetchGameInfo(name.trim(), path, platform, context);
        } finally {
            messageProperty.set(null);
        }
    }

    private Opt<GameInfo> doFetchGameInfo(String name, Path path, GamePlatform platform, SearchContext context) throws Exception {
        checkStopped();
        final ImmutableList<SearchResult> searchResults = searchGames(name, platform, context);
        checkStopped();

        if (searchResults.isEmpty()) {
            return handleNoSearchResults(name, path, platform, context);
        }

        if (searchResults.size() > 1) {
            return handleMultipleSearchResults(name, path, platform, context, searchResults);
        }

        final SearchResult singleSearchResult = searchResults.get(0);
        return Opt.of(fetchGameInfoFromSearchResult(singleSearchResult));
    }

    private ImmutableList<SearchResult> searchGames(String name, GamePlatform platform, SearchContext context) throws Exception {
        message("Searching '%s'...", name);
        fetchingProperty.set(true);
        final ImmutableList<SearchResult> searchResults;
        try {
            searchResults = gameInfoProvider.searchGames(name, platform);
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

    private Opt<GameInfo> handleNoSearchResults(String name, Path path, GamePlatform platform, SearchContext context) throws Exception {
        assertNotAutoSkip();

        final NoSearchResultsDialogParams params = NoSearchResultsDialogParams.builder()
            .providerName(gameInfoProvider.getProviderType().getName())
            .name(name)
            .path(path)
            .platform(platform)
            .canProceedWithout(canProceedWithout)
            .build();
        final DialogChoice choice = dialogService.noSearchResultsDialog(params);
        return choice.resolve(new DefaultDialogChoiceResolver() {
            @Override
            public Opt<GameInfo> newName(String newName) throws Exception {
                return fetchGameInfo(newName, path, platform, context);
            }
        });
    }

    private Opt<GameInfo> handleMultipleSearchResults(String name,
                                                      Path path,
                                                      GamePlatform platform,
                                                      SearchContext context,
                                                      ImmutableList<SearchResult> searchResults) throws Exception {
        assertNotAutoSkip();

        final MultipleSearchResultsDialogParams params = MultipleSearchResultsDialogParams.builder()
            .providerName(gameInfoProvider.getProviderType().getName())
            .name(name)
            .path(path)
            .platform(platform)
            .searchResults(searchResults)
            .canProceedWithout(canProceedWithout)
            .build();
        final DialogChoice choice = dialogService.multipleSearchResultsDialog(params);
        return choice.resolve(new DefaultDialogChoiceResolver() {
            @Override
            public Opt<GameInfo> newName(String newName) throws Exception {
                // Add all current search results to excluded list.
                final ImmutableList<String> searchResultNames = getSearchResultNames(searchResults);
                context.addExcludedNames(searchResultNames);
                return fetchGameInfo(newName, path, platform, context);
            }

            @Override
            public Opt<GameInfo> choose(SearchResult chosenSearchResult) throws Exception {
                // Add all other search results to excluded list.
                final ImmutableList<String> searchResultNames = getSearchResultNames(searchResults);
                final ImmutableList<String> excludedNames = searchResultNames.newWithout(chosenSearchResult.getName());
                context.addExcludedNames(excludedNames);
                return Opt.of(fetchGameInfoFromSearchResult(chosenSearchResult));
            }
        });
    }

    private ImmutableList<String> getSearchResultNames(ImmutableList<SearchResult> searchResults) {
        return searchResults.collect(SearchResult::getName);
    }

    private GameInfo fetchGameInfoFromSearchResult(SearchResult searchResult) throws Exception {
        fetchingProperty.set(true);
        try {
            message("Fetching '%s'...", searchResult.getName());
            final GameInfo gameInfo = gameInfoProvider.getGameInfo(searchResult);
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
        final String messageWithProvider = gameInfoProvider.getProviderType().getName() + ": " + message;
        messageProperty.set(messageWithProvider);
    }

    private void checkStopped() {
        if (Thread.interrupted()) {
            message("Stopping...");
            throw new GameDexException("Stopped.");
        }
    }
}
