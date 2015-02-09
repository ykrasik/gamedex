package com.github.ykrasik.indexter.games.manager.flow;

import com.github.ykrasik.indexter.AbstractService;
import com.github.ykrasik.indexter.exception.IndexterException;
import com.github.ykrasik.indexter.exception.RunnableThrows;
import com.github.ykrasik.indexter.games.datamodel.GamePlatform;
import com.github.ykrasik.indexter.games.datamodel.ImageData;
import com.github.ykrasik.indexter.games.datamodel.info.GameInfo;
import com.github.ykrasik.indexter.games.datamodel.info.SearchResult;
import com.github.ykrasik.indexter.games.datamodel.persistence.Game;
import com.github.ykrasik.indexter.games.datamodel.persistence.Library;
import com.github.ykrasik.indexter.games.info.GameInfoProvider;
import com.github.ykrasik.indexter.games.info.GameInfoService;
import com.github.ykrasik.indexter.games.manager.exclude.ExcludedPathManager;
import com.github.ykrasik.indexter.games.manager.flow.dialog.DialogManager;
import com.github.ykrasik.indexter.games.manager.flow.dialog.MultipleSearchResultsDialogParams;
import com.github.ykrasik.indexter.games.manager.flow.dialog.NoSearchResultsDialogParams;
import com.github.ykrasik.indexter.games.manager.flow.dialog.choice.DialogChoice;
import com.github.ykrasik.indexter.games.manager.flow.dialog.choice.DialogChoiceResolverAdapter;
import com.github.ykrasik.indexter.games.manager.game.GameManager;
import com.github.ykrasik.indexter.games.manager.library.LibraryManager;
import com.github.ykrasik.indexter.optional.Optionals;
import com.github.ykrasik.indexter.util.FileUtils;
import com.github.ykrasik.indexter.util.ListUtils;
import com.github.ykrasik.indexter.util.PlatformUtils;
import javafx.beans.property.*;
import javafx.concurrent.Task;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
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
    @NonNull private final GameInfoService metacriticInfoService;
    @NonNull private final GameInfoService giantBombInfoService;
    @NonNull private final DialogManager dialogManager;

    private final StringProperty messageProperty = new SimpleStringProperty();
    private final DoubleProperty progressProperty = new SimpleDoubleProperty();
    private final DoubleProperty fetchProgressProperty = new SimpleDoubleProperty();

    private ExecutorService executorService;

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
    public Task<Void> refreshLibraries() {
        return submit(this::doRefreshLibraries);
    }

    private void doRefreshLibraries() throws Exception {
        info("Refreshing libraries...");

        final List<Library> libraries = libraryManager.getAllLibraries();
        for (Library library : libraries) {
            refreshLibrary(library);
        }

        info("Finished refreshing libraries.");
    }

    @Override
    public Task<Void> cleanupGames() {
        return submit(this::doCleanupGames);
    }

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

        info("Detected %d obsolete games.", obsoleteGames.size());
        gameManager.deleteGames(obsoleteGames);
        info("Removed %d obsolete games.", obsoleteGames.size());
        setProgress(0, 1);
    }

    @Override
    public Task<Void> processPath(Library library, Path path) {
        return submit(() -> doProcessPath(library, path));
    }

    private void refreshLibrary(Library library) throws Exception {
        info("Refreshing library: '%s'[%s]\n", library.getName(), library.getPlatform());

        final List<Path> directories = FileUtils.listChildDirectories(library.getPath());
        final int total = directories.size();
        for (int i = 0; i < total; i++) {
            setProgress(i, total);
            final Path path = directories.get(i);
            if (doProcessPath(library, path)) {
                info("");
            }
        }

        info("Finished refreshing library: '%s'[%s]", library.getName(), library.getPlatform());
        setProgress(0, 1);
    }

    private boolean doProcessPath(Library library, Path path) throws Exception {
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

        if (tryCreateLibrary(path, library.getPlatform())) {
            return true;
        }

        info("Processing path: %s...", path);
        final String name = getName(path);
        try {
            addPath(library, path, name);
        } catch (SkipException e) {
            info("Skipping...");
        } catch (ExcludeException e) {
            info("Excluding...");
            excludedPathManager.addExcludedPath(path);
        }
        info("Finished Processing %s.", path);
        return true;
    }

    private boolean tryCreateLibrary(Path path, GamePlatform platform) throws Exception {
        if (!FileUtils.hasChildDirectories(path)) {
            // Only directories that have sub-directories can be libraries.
            return false;
        }

        if (!dialogManager.shouldCreateLibraryDialog(path)) {
            return false;
        }

        final Optional<String> libraryName = dialogManager.libraryNameDialog(path, platform);
        if (!libraryName.isPresent()) {
            return false;
        }

        final Library library = libraryManager.createLibrary(libraryName.get(), path, platform);
        info("New library created: '%s'", library.getName());
        refreshLibrary(library);
        return true;
    }

    private String getName(Path path) {
        // Remove all metaData enclosed with '[]' from the game name.
        final String rawName = path.getFileName().toString();
        final String nameWithoutMetadata = META_DATA_PATTERN.matcher(rawName).replaceAll("");
        return SPACE_PATTERN.matcher(nameWithoutMetadata.trim()).replaceAll(" ");
    }

    private void addPath(Library library, Path path, String name) throws Exception {
        final GamePlatform platform = library.getPlatform();

        final SearchContext metacriticSearchContext = new SearchContext(metacriticInfoService, path, platform);
        final Optional<GameInfo> metacriticGameOptional = fetchGameInfo(metacriticSearchContext, name);
        if (metacriticGameOptional.isPresent()) {
            final GameInfo metacriticGame = metacriticGameOptional.get();
            LOG.debug("Metacritic gameInfo: {}", metacriticGame);

            // Transfer excluded searches from metacritic search, and use Metacritic's name - more likely to be accurate.
            final SearchContext giantBombSearchContext = new SearchContext(giantBombInfoService, path, platform);
            giantBombSearchContext.addExcludedNames(metacriticSearchContext.getExcludedNames());
            final String metacriticName = metacriticGame.getName();

            final Optional<GameInfo> giantBombGameOptional = fetchGameInfo(giantBombSearchContext, metacriticName);
            final GameInfo mergedGame;
            if (giantBombGameOptional.isPresent()) {
                final GameInfo giantBombGame = giantBombGameOptional.get();
                LOG.debug("GiantBomb gameInfo: {}", giantBombGame);
                mergedGame = mergeGameInfos(metacriticGame, giantBombGame);
            } else {
                info("Game not found on GiantBomb.");
                mergedGame = metacriticGame;
            }

            final Game game = gameManager.addGame(mergedGame, path, platform);
            libraryManager.addGameToLibrary(game, library);
        }
    }

    private Optional<GameInfo> fetchGameInfo(SearchContext searchContext, String name) throws Exception {
        final List<SearchResult> searchResults = searchGames(searchContext, name);

        if (searchResults.isEmpty()) {
            return handleNoSearchResults(searchContext, name);
        }

        if (searchResults.size() > 1) {
            return handleMultipleSearchResults(searchContext, name, searchResults);
        }

        final SearchResult singleSearchResult = searchResults.get(0);
        return Optional.of(fetchGameInfoFromSearchResult(searchContext, singleSearchResult));
    }

    private List<SearchResult> searchGames(SearchContext searchContext, String name) throws Exception {
        final GameInfoService gameInfoService = searchContext.getGameInfoService();
        final GamePlatform platform = searchContext.getPlatform();

        info("%s: Searching '%s'...", gameInfoService.getProvider().getName(), name);
        startFetching();
        final List<SearchResult> searchResults = gameInfoService.searchGames(name, platform);
        finishFetching();
        info("Results: %d", searchResults.size());

        final Collection<String> excludedNames = searchContext.getExcludedNames();
        if (searchResults.size() <= 1 || !searchContext.isCanExclude() || excludedNames.isEmpty()) {
            return searchResults;
        }

        info("Filtering previously encountered search results...");
        final List<SearchResult> filteredSearchResults = ListUtils.filter(searchResults, result -> !excludedNames.contains(result.getName()));
        if (!filteredSearchResults.isEmpty()) {
            info("Results: %d", filteredSearchResults.size());
            return filteredSearchResults;
        } else {
            info("No search results after filtering, reverting...");
            return searchResults;
        }
    }

    private Optional<GameInfo> handleNoSearchResults(SearchContext searchContext, String name) throws Exception {
        final GameInfoProvider provider = searchContext.getGameInfoService().getProvider();
        final NoSearchResultsDialogParams params = NoSearchResultsDialogParams.builder()
            .providerName(provider.getName())
            .name(name)
            .platform(searchContext.getPlatform())
            .path(searchContext.getPath())
            .canProceedWithout(provider != GameInfoProvider.METACRITIC)
            .build();
        final DialogChoice choice = dialogManager.noSearchResultsDialog(params);
        return choice.resolve(new DialogChoiceResolverAdapter() {
            @Override
            public Optional<GameInfo> newName(String newName) throws Exception {
                return fetchGameInfo(searchContext, newName);
            }
        });
    }

    private Optional<GameInfo> handleMultipleSearchResults(SearchContext searchContext, String name, List<SearchResult> searchResults) throws Exception {
        final List<SearchResult> sortedSearchResults = new ArrayList<>(searchResults);
        Collections.sort(sortedSearchResults, SearchResultComparators.releaseDateComparator());

        final GameInfoProvider provider = searchContext.getGameInfoService().getProvider();
        final MultipleSearchResultsDialogParams params = MultipleSearchResultsDialogParams.builder()
            .providerName(provider.getName())
            .name(name)
            .platform(searchContext.getPlatform())
            .path(searchContext.getPath())
            .searchResults(sortedSearchResults)
            .canProceedWithout(provider != GameInfoProvider.METACRITIC)
            .build();
        final DialogChoice choice = dialogManager.multipleSearchResultsDialog(params);
        return choice.resolve(new DialogChoiceResolverAdapter() {
            @Override
            public Optional<GameInfo> newName(String newName) throws Exception {
                // Add all current search results to excluded list.
                final List<String> searchResultNames = getSearchResultNames(searchResults);
                searchContext.addExcludedNames(searchResultNames);
                return fetchGameInfo(searchContext, newName);
            }

            @Override
            public Optional<GameInfo> choose(SearchResult chosenSearchResult) throws Exception {
                // Add all other search results to excluded list.
                final List<String> searchResultNames = getSearchResultNames(searchResults);
                final List<String> excludedNames = ListUtils.takeAllExcept(searchResultNames, chosenSearchResult.getName());
                searchContext.addExcludedNames(excludedNames);
                return Optional.of(fetchGameInfoFromSearchResult(searchContext, chosenSearchResult));
            }
        });
    }

    private List<String> getSearchResultNames(List<SearchResult> searchResults) {
        return ListUtils.map(searchResults, SearchResult::getName);
    }

    private GameInfo fetchGameInfoFromSearchResult(SearchContext searchContext, SearchResult searchResult) throws Exception {
        final GameInfoService gameInfoService = searchContext.getGameInfoService();
        final String providerName = gameInfoService.getProvider().getName();

        info("%s: Fetching '%s'...", providerName, searchResult.getName());
        startFetching();
        final GameInfo gameInfo = gameInfoService.getGameInfo(searchResult).orElseThrow(
            () -> new IndexterException("Specific %s search found nothing: %s", providerName, searchResult)
        );
        finishFetching();
        info("Done.");

        return gameInfo;
    }

    private GameInfo mergeGameInfos(GameInfo metacriticGame, GameInfo giantBombGame) {
        // TODO: Consider only using giantBomb genre if present.
        final Set<String> genres = new HashSet<>();
        genres.addAll(metacriticGame.getGenres());
        genres.addAll(giantBombGame.getGenres());

        final Optional<ImageData> thumbnail = Optionals.or(giantBombGame.getThumbnail(), metacriticGame.getThumbnail());

        // TODO: For now, only save the giantBomb detailUrl
        return GameInfo.builder()
            .detailUrl(giantBombGame.getDetailUrl())
            .name(metacriticGame.getName())
            .description(Optionals.or(giantBombGame.getDescription(), metacriticGame.getDescription()))
            .releaseDate(Optionals.or(metacriticGame.getReleaseDate(), giantBombGame.getReleaseDate()))
            .criticScore(metacriticGame.getCriticScore())
            .userScore(metacriticGame.getUserScore())
            .url(metacriticGame.getUrl())
            .thumbnail(thumbnail)
            .poster(Optionals.or(giantBombGame.getPoster(), thumbnail))
            .genres(new ArrayList<>(genres))
            .build();
    }

    // TODO: I don't like that platformUtils.runLater is called from here.
    private void info(String format, Object... args) {
        PlatformUtils.runLaterIfNecessary(() -> messageProperty.setValue(String.format(format, args)));
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
        executorService.submit(task);
        return task;
    }
}
