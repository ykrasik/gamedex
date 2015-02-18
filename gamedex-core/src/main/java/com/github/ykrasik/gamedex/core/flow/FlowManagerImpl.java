package com.github.ykrasik.gamedex.core.flow;

import com.github.ykrasik.gamedex.common.exception.GameDexException;
import com.github.ykrasik.gamedex.common.exception.RunnableThrows;
import com.github.ykrasik.gamedex.common.service.AbstractService;
import com.github.ykrasik.gamedex.common.util.FileUtils;
import com.github.ykrasik.gamedex.common.util.ListUtils;
import com.github.ykrasik.gamedex.common.util.PlatformUtils;
import com.github.ykrasik.gamedex.core.dialog.DialogManager;
import com.github.ykrasik.gamedex.core.dialog.MultipleSearchResultsDialogParams;
import com.github.ykrasik.gamedex.core.dialog.NoSearchResultsDialogParams;
import com.github.ykrasik.gamedex.core.dialog.choice.DialogChoice;
import com.github.ykrasik.gamedex.core.dialog.choice.DialogChoiceResolverAdapter;
import com.github.ykrasik.gamedex.core.exclude.ExcludedPathManager;
import com.github.ykrasik.gamedex.core.game.GameManager;
import com.github.ykrasik.gamedex.core.library.LibraryManager;
import com.github.ykrasik.gamedex.core.ui.library.LibraryDef;
import com.github.ykrasik.gamedex.datamodel.GamePlatform;
import com.github.ykrasik.gamedex.datamodel.flow.LibraryHierarchy;
import com.github.ykrasik.gamedex.datamodel.persistence.Game;
import com.github.ykrasik.gamedex.datamodel.persistence.Library;
import com.github.ykrasik.gamedex.datamodel.provider.GameInfo;
import com.github.ykrasik.gamedex.datamodel.provider.SearchResult;
import com.github.ykrasik.gamedex.datamodel.provider.UnifiedGameInfo;
import com.github.ykrasik.gamedex.provider.GameInfoProvider;
import com.github.ykrasik.gamedex.provider.GameInfoProviderType;
import com.github.ykrasik.opt.Opt;
import javafx.beans.property.*;
import javafx.concurrent.Task;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

/**
 * @author Yevgeny Krasik
 */
@RequiredArgsConstructor
public class FlowManagerImpl extends AbstractService implements FlowManager {
    private static final Pattern META_DATA_PATTERN = Pattern.compile("(\\[.*?\\])|(-)");
    private static final Pattern SPACE_PATTERN = Pattern.compile("\\s+");

    @NonNull private final LibraryManager libraryManager;
    @NonNull private final ExcludedPathManager excludedPathManager;
    @NonNull private final GameManager gameManager;
    @NonNull private final GameInfoProvider metacriticInfoService;
    @NonNull private final GameInfoProvider giantBombInfoService;
    @NonNull private final DialogManager dialogManager;

    private final StringProperty messageProperty = new SimpleStringProperty();
    private final DoubleProperty progressProperty = new SimpleDoubleProperty();
    private final DoubleProperty fetchProgressProperty = new SimpleDoubleProperty();

    private ExecutorService executorService;

    @Setter
    private boolean autoSkip;

    @Override
    protected void doStart() {
        executorService = Executors.newSingleThreadExecutor();
    }

    @Override
    protected void doStop() throws Exception {
        executorService.shutdownNow();
    }

    @Override
    public ReadOnlyStringProperty messageProperty() {
        return messageProperty;
    }

    @Override
    public ReadOnlyDoubleProperty progressProperty() {
        return progressProperty;
    }

    @Override
    public ReadOnlyDoubleProperty fetchProgressProperty() {
        return fetchProgressProperty;
    }

    @Override
    public void stopTask(Task<Void> task) {
        info("Cancelled.");
        progressProperty.setValue(0.0);
        fetchProgressProperty.setValue(0.0);
    }

    @Override
    public Task<Void> refreshLibraries() {
        return submit(this::doRefreshLibraries);
    }

    private void doRefreshLibraries() throws Exception {
        info("Refreshing libraries...");

        final List<Library> libraries = libraryManager.getAllLibraries();
        for (Library library : libraries) {
            final LibraryHierarchy libraryHierarchy = new LibraryHierarchy(library);
            refreshCurrentLibrary(libraryHierarchy);
        }

        info("Finished refreshing libraries.");
    }

    @Override
    public Task<Void> cleanupGames() {
        return submit(this::doCleanupGames);
    }

    // TODO: Make this a total cleanup? libraries, excluded, evertyhing?
    private void doCleanupGames() {
        info("Cleaning up games...");

        final List<Game> obsoleteGames = new ArrayList<>();
        final List<Game> games = gameManager.getAllGames();
        for (int i = 0; i < games.size(); i++) {
            setProgress(i, games.size());
            final Game game = games.get(i);
            final Path path = game.getPath();
            if (!Files.exists(path)) {
                info("Obsolete path detected: %s", path);
                obsoleteGames.add(game);
            }
        }

        gameManager.deleteGames(obsoleteGames);
        info("Removed %d obsolete games.", obsoleteGames.size());
        setProgress(0, 1);
    }

    @Override
    public Task<Void> processPath(Library library, Path path) {
        final LibraryHierarchy libraryHierarchy = new LibraryHierarchy(library);
        return submit(() -> doProcessPath(libraryHierarchy, path));
    }

    private void refreshCurrentLibrary(LibraryHierarchy libraryHierarchy) throws Exception {
        final Library library = libraryHierarchy.getCurrentLibrary();
        info("Refreshing library: '%s'[%s]\n", library.getName(), library.getPlatform());

        final List<Path> directories = FileUtils.listChildDirectories(library.getPath());
        final int total = directories.size();
        for (int i = 0; i < total; i++) {
            setProgress(i, total);
            final Path path = directories.get(i);
            if (doProcessPath(libraryHierarchy, path)) {
                info("");
            }
        }

        info("Finished refreshing library: '%s'[%s]", library.getName(), library.getPlatform());
        setProgress(0, 1);
    }

    private boolean doProcessPath(LibraryHierarchy libraryHierarchy, Path path) throws Exception {
        if (libraryManager.isLibrary(path)) {
            LOG.info("{} is a library, skipping...", path);
            return false;
        }

        if (excludedPathManager.isExcluded(path)) {
            LOG.info("{} is excluded, skipping...", path);
            return false;
        }

        if (gameManager.isGame(path)) {
            LOG.info("{} is already mapped, skipping...", path);
            return false;
        }

        if (!autoSkip && tryCreateLibrary(path, libraryHierarchy)) {
            return true;
        }

        info("Processing: %s...", path);
        final String name = getName(path);
        try {
            addPath(libraryHierarchy, path, name);
        } catch (SkipException e) {
            info("Skipping...");
        } catch (ExcludeException e) {
            info("Excluding...");
            excludedPathManager.addExcludedPath(path);
        }
        info("Finished processing %s.", path);
        return true;
    }

    private boolean tryCreateLibrary(Path path, LibraryHierarchy libraryHierarchy) throws Exception {
        if (!FileUtils.hasChildDirectories(path) || FileUtils.hasChildFiles(path)) {
            // Only directories that have sub-directories and no files can be libraries.
            return false;
        }

        final List<Path> children = FileUtils.listChildDirectories(path);
        final Opt<LibraryDef> libraryDefOpt = dialogManager.createLibraryDialog(path, children, libraryHierarchy.getPlatform());
        if (libraryDefOpt.isEmpty()) {
            return false;
        }

        final LibraryDef libraryDef = libraryDefOpt.get();
        final Library library = libraryManager.createLibrary(libraryDef.getName(), path, libraryDef.getPlatform());
        info("New library created: '%s'", library.getName());

        libraryHierarchy.pushLibrary(library);
        refreshCurrentLibrary(libraryHierarchy);
        libraryHierarchy.popLibrary();
        return true;
    }

    private String getName(Path path) {
        // Remove all metaData enclosed with '[]' from the game name.
        final String rawName = path.getFileName().toString();
        final String nameWithoutMetadata = META_DATA_PATTERN.matcher(rawName).replaceAll("");
        return SPACE_PATTERN.matcher(nameWithoutMetadata).replaceAll(" ");
    }

    private void addPath(LibraryHierarchy libraryHierarchy, Path path, String name) throws Exception {
        if (name.isEmpty()) {
            info("Empty name provided.");
            throw new SkipException();
        }

        final GamePlatform platform = libraryHierarchy.getPlatform();

        final SearchContext metacriticSearchContext = new SearchContext(metacriticInfoService, path, platform);
        final Opt<GameInfo> metacriticGameOpt = fetchGameInfo(metacriticSearchContext, name.trim());
        if (metacriticGameOpt.isPresent()) {
            final GameInfo metacriticGame = metacriticGameOpt.get();
            LOG.debug("Metacritic gameInfo: {}", metacriticGame);

            // Transfer excluded searches from metacritic search, and use Metacritic's name - more likely to be accurate.
            final SearchContext giantBombSearchContext = new SearchContext(giantBombInfoService, path, platform);
            giantBombSearchContext.addExcludedNames(metacriticSearchContext.getExcludedNames());
            final String metacriticName = metacriticGame.getName();

            final Opt<GameInfo> giantBombGameOpt = fetchGameInfo(giantBombSearchContext, metacriticName);
            if (!giantBombGameOpt.isPresent()) {
                info("Game not found on GiantBomb.");
            }

            final UnifiedGameInfo gameInfo = UnifiedGameInfo.from(metacriticGame, giantBombGameOpt);
            final Game game = gameManager.addGame(gameInfo, path, platform);
            libraryManager.addGameToLibraryHierarchy(game, libraryHierarchy);
        }
    }

    private Opt<GameInfo> fetchGameInfo(SearchContext searchContext, String name) throws Exception {
        final String trimmedName = name.trim();
        final List<SearchResult> searchResults = searchGames(searchContext, trimmedName);

        if (searchResults.isEmpty()) {
            return handleNoSearchResults(searchContext, trimmedName);
        }

        if (searchResults.size() > 1) {
            return handleMultipleSearchResults(searchContext, trimmedName, searchResults);
        }

        final SearchResult singleSearchResult = searchResults.get(0);
        return Opt.of(fetchGameInfoFromSearchResult(searchContext, singleSearchResult));
    }

    private List<SearchResult> searchGames(SearchContext searchContext, String name) throws Exception {
        final GameInfoProvider gameInfoProvider = searchContext.getGameInfoProvider();
        final String providerName = gameInfoProvider.getProviderType().getName();
        final GamePlatform platform = searchContext.getPlatform();

        info("%s: Searching '%s'...", providerName, name);
        startFetching();
        final List<SearchResult> searchResults = gameInfoProvider.searchGames(name, platform);
        finishFetching();
        info("%s: %d results for '%s'.", providerName, searchResults.size(), name);

        final Collection<String> excludedNames = searchContext.getExcludedNames();
        if (searchResults.size() <= 1 || !searchContext.isCanExclude() || excludedNames.isEmpty()) {
            return searchResults;
        }

        info("Filtering previously encountered search results...");
        final List<SearchResult> filteredSearchResults = ListUtils.filter(searchResults, result -> !excludedNames.contains(result.getName()));
        if (!filteredSearchResults.isEmpty()) {
            info("%s: %d remaining results.", providerName, filteredSearchResults.size());
            return filteredSearchResults;
        } else {
            info("%s: No search results after filtering, reverting...", providerName);
            return searchResults;
        }
    }

    private Opt<GameInfo> handleNoSearchResults(SearchContext searchContext, String name) throws Exception {
        if (autoSkip) {
            info("Autoskipping...");
            throw new SkipException();
        }

        final GameInfoProviderType providerType = searchContext.getGameInfoProvider().getProviderType();
        final NoSearchResultsDialogParams params = NoSearchResultsDialogParams.builder()
            .providerName(providerType.getName())
            .name(name)
            .platform(searchContext.getPlatform())
            .path(searchContext.getPath())
            .canProceedWithout(providerType != GameInfoProviderType.METACRITIC)
            .build();
        final DialogChoice choice = dialogManager.noSearchResultsDialog(params);
        return choice.resolve(new DialogChoiceResolverAdapter() {
            @Override
            public Opt<GameInfo> newName(String newName) throws Exception {
                return fetchGameInfo(searchContext, newName);
            }
        });
    }

    private Opt<GameInfo> handleMultipleSearchResults(SearchContext searchContext, String name, List<SearchResult> searchResults) throws Exception {
        if (autoSkip) {
            info("Autoskipping...");
            throw new SkipException();
        }

        final List<SearchResult> sortedSearchResults = new ArrayList<>(searchResults);
        Collections.sort(sortedSearchResults, SearchResultComparators.releaseDateDesc());

        final GameInfoProviderType providerType = searchContext.getGameInfoProvider().getProviderType();
        final MultipleSearchResultsDialogParams params = MultipleSearchResultsDialogParams.builder()
            .providerName(providerType.getName())
            .name(name)
            .platform(searchContext.getPlatform())
            .path(searchContext.getPath())
            .searchResults(sortedSearchResults)
            .canProceedWithout(providerType != GameInfoProviderType.METACRITIC)
            .build();
        final DialogChoice choice = dialogManager.multipleSearchResultsDialog(params);
        return choice.resolve(new DialogChoiceResolverAdapter() {
            @Override
            public Opt<GameInfo> newName(String newName) throws Exception {
                // Add all current search results to excluded list.
                final List<String> searchResultNames = getSearchResultNames(searchResults);
                searchContext.addExcludedNames(searchResultNames);
                return fetchGameInfo(searchContext, newName);
            }

            @Override
            public Opt<GameInfo> choose(SearchResult chosenSearchResult) throws Exception {
                // Add all other search results to excluded list.
                final List<String> searchResultNames = getSearchResultNames(searchResults);
                final List<String> excludedNames = ListUtils.takeAllExcept(searchResultNames, chosenSearchResult.getName());
                searchContext.addExcludedNames(excludedNames);
                return Opt.of(fetchGameInfoFromSearchResult(searchContext, chosenSearchResult));
            }
        });
    }

    private List<String> getSearchResultNames(List<SearchResult> searchResults) {
        return ListUtils.map(searchResults, SearchResult::getName);
    }

    private GameInfo fetchGameInfoFromSearchResult(SearchContext searchContext, SearchResult searchResult) throws Exception {
        final GameInfoProvider gameInfoProvider = searchContext.getGameInfoProvider();
        final String providerName = gameInfoProvider.getProviderType().getName();

        info("%s: Fetching '%s'...", providerName, searchResult.getName());
        startFetching();
        final GameInfo gameInfo = gameInfoProvider.getGameInfo(searchResult).getOrElseThrow(
            () -> new GameDexException("Specific %s search found nothing: %s", providerName, searchResult)
        );
        finishFetching();
        info("%s: Done.", providerName);
        LOG.debug("GameInfo: {}", gameInfo);

        return gameInfo;
    }

    // TODO: I don't like that platformUtils.runLater is called from here.
    private void info(String format, Object... args) {
        final String message = String.format(format, args);
        LOG.info(message);
        PlatformUtils.runLaterIfNecessary(() -> messageProperty.setValue(message));
    }

    private void setProgress(int current, int total) {
        PlatformUtils.runLaterIfNecessary(() -> progressProperty.setValue((double) current / total));
    }

    private void startFetching() {
        setFetchProgress(-1.0);
    }

    private void finishFetching() {
        setFetchProgress(0);
    }

    private void setFetchProgress(double value) {
        PlatformUtils.runLaterIfNecessary(() -> fetchProgressProperty.setValue(value));
    }

    private Task<Void> submit(RunnableThrows runnable) {
        final Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                runnable.run();
                return null;
            }
        };
        task.setOnFailed(e -> dialogManager.showException(task.getException()));
        executorService.submit(task);
        return task;
    }
}
