package com.gitlab.ykrasik.gamedex.core.scan

import com.github.ykrasik.gamedex.common.collapseSpaces
import com.github.ykrasik.gamedex.common.emptyToNull
import com.github.ykrasik.gamedex.common.logger
import com.github.ykrasik.gamedex.datamodel.Game
import com.github.ykrasik.gamedex.datamodel.Library
import com.github.ykrasik.gamedex.datamodel.provider.GameData
import com.gitlab.ykrasik.gamedex.core.UserPreferences
import com.gitlab.ykrasik.gamedex.core.controller.GameController
import com.gitlab.ykrasik.gamedex.provider.DataProviderService
import com.gitlab.ykrasik.gamedex.provider.SearchResult
import javafx.concurrent.Task
import java.nio.file.Path
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 12/10/2016
 * Time: 12:52
 */
@Singleton
class LibraryScanner @Inject constructor(
    private val userPreferences: UserPreferences,
    private val pathDetector: PathDetector,
    private val gameController: GameController,
    private val providerService: DataProviderService
) {
    private val log by logger()
    private val metaDataRegex = "(\\[.*?\\])|(-)".toRegex()

    fun refresh(library: Library): Task<Unit> = object : Task<Unit>() {
        init {
            messageProperty().addListener { observableValue, oldValue, newValue ->
                log.info { newValue }
            }
        }

        override fun call() {
            updateMessage("Refreshing library: '$library'...")
            val newPaths = pathDetector.detectNewPaths(library.path)
            val newGames = newPaths.mapIndexedNotNull { i, path ->
                if (isStopped()) return@mapIndexedNotNull

                updateProgress(i.toLong(), newPaths.size.toLong())
                processPath(path, library)
            }
            updateMessage("Done refreshing library: '$library'. Added ${newGames.size} new games.")
        }

        private fun processPath(path: Path, library: Library): Game? {
            val name = path.normalizeName().emptyToNull() ?: return null
            val platform = library.platform

            val gameData = providerService.fetch(name, platform) { results ->
                chooseSearchResult(results)
            } ?: return null

            // FIXME: TEMP!!!
            val d = GameData(gameData.name, gameData.description, gameData.releaseDate, gameData.criticScore, gameData.userScore,
                null, null, gameData.genres, "", null)

            val game = gameController.add(d, path, library)
            updateMessage("[$path] Done: $game")
            return game
        }

        private fun chooseSearchResult(results: List<SearchResult>): SearchResult? {
            // TODO: Display dialog
            return results.first()
        }

        private fun isStopped(): Boolean = this.isCancelled || Thread.interrupted()

        // Remove all metaData enclosed with '[]' from the file name and collapse all spaces into a single space.
        private fun Path.normalizeName(): String = metaDataRegex.replace(fileName.toString(), "").collapseSpaces()
    }

//    @Throws(Exception::class)
//    private fun doProcessPath(libraryHierarchy: LibraryHierarchy, path: Path, name: String): ProcessPathReturnValue {
//        if (name.isEmpty()) {
//            message("Empty name provided.")
//            throw SkipException()
//        }
//
//        val platform = libraryHierarchy.getPlatform()
//
//        val searchContext = SearchContext(path, platform)
//        val metacriticGameOpt = fetchGameInfo(metacriticManager, name, searchContext)
//        if (metacriticGameOpt.isEmpty()) {
//            message("Game not found on Metacritic.")
//            throw SkipException()
//        }
//
//        val metacriticGame = metacriticGameOpt.get()
//        log.debug("Metacritic gameInfo: {}", metacriticGame)
//
//        val metacriticName = metacriticGame.getName()
//        val giantBombGameOpt = fetchGameInfo(giantBombManager, metacriticName, searchContext)
//        if (!giantBombGameOpt.isDefined()) {
//            message("Game not found on GiantBomb.")
//        }
//
//        val gameInfo = GameData.from(metacriticGame, giantBombGameOpt)
//        val game = gameManager.addGame(gameInfo, path, platform)
//        libraryManager.addGameToLibraryHierarchy(game, libraryHierarchy)
//        return OK
//    }
//
//    @Throws(Exception::class)
//    override fun fetchGameInfo(name: String, context: SearchContext): Opt<GameInfo> {
//        return doFetchGameInfo(name.trim { it <= ' ' }, context)
//    }
//
//    @Throws(Exception::class)
//    private fun doFetchGameInfo(name: String, context: SearchContext): Opt<GameInfo> {
//        assertNotStopped()
//        val searchResults = searchGames(name, context)
//        assertNotStopped()
//
//        if (searchResults.size() == 1) {
//            return Opt.some(fetchGameInfoFromSearchResult(searchResults.get(0)))
//        }
//
//        assertNotAutoSkip()
//        val choice = gameSearchScreen.show(name, context.path(), gameInfoProvider.info, searchResults)
//        when (choice.type()) {
//            GameSearchChoiceType.SELECT_RESULT -> {
//                val selectedResult = choice.searchResult().get()
//                log.info("Result selected: {}", selectedResult)
//
//                // Add all search results except the selected one to excluded list.
//                val resultsToExclude = searchResults.newWithout(selectedResult)
//                context.addExcludedNames(resultsToExclude.collect(Function<SearchResult, String> { it.getName() }))
//                return Opt.some(fetchGameInfoFromSearchResult(selectedResult))
//            }
//
//            GameSearchChoiceType.NEW_NAME -> {
//                log.info("New name requested: {}", choice.newName().get())
//
//                // Add all current search results to excluded list.
//                context.addExcludedNames(searchResults.collect(Function<SearchResult, String> { it.getName() }))
//                return doFetchGameInfo(choice.newName().get(), context)
//            }
//
//            GameSearchChoiceType.SKIP -> {
//                log.info("Skip selected.")
//                throw SkipException()
//            }
//            GameSearchChoiceType.EXCLUDE -> {
//                log.info("Exclude selected.")
//                throw ExcludeException()
//            }
//            GameSearchChoiceType.PROCEED_ANYWAY -> {
//                log.info("Proceed anyway selected.")
//                return Opt.none()
//            }
//            else -> throw IllegalStateException("Invalid choice type: " + choice.type())
//        }
//    }
//
//    @Throws(Exception::class)
//    private fun searchGames(name: String, context: SearchContext): ImmutableList<SearchResult> {
//        message("Searching '%s'...", name)
//        fetchingProperty.set(true)
//        val searchResults: ImmutableList<SearchResult>
//        try {
//            searchResults = gameInfoProvider.search(name, context.platform())
//        } finally {
//            fetchingProperty.set(false)
//        }
//        message("Found %d results for '%s'.", searchResults.size(), name)
//
//        val excludedNames = context.getExcludedNames()
//        if (searchResults.size() <= 1 || excludedNames.isEmpty()) {
//            return searchResults
//        }
//
//        message("Filtering previously encountered search results...")
//        val filteredSearchResults = searchResults.select({ result -> !excludedNames.contains(result.name) })
//        if (!filteredSearchResults.isEmpty()) {
//            message("%d remaining results.", filteredSearchResults.size())
//            return filteredSearchResults
//        } else {
//            message("No search results after filtering, reverting...")
//            return searchResults
//        }
//    }
//
//    @Throws(Exception::class)
//    private fun fetchGameInfoFromSearchResult(searchResult: SearchResult): GameInfo {
//        fetchingProperty.set(true)
//        try {
//            message("Fetching '%s'...", searchResult.name)
//            val gameInfo = gameInfoProvider.fetch(searchResult)
//            message("Done.")
//            return gameInfo
//        } finally {
//            fetchingProperty.set(false)
//        }
//    }
//
//    private fun assertNotAutoSkip() {
//        if (configService.isAutoSkip()) {
//            message("AutoSkip is on.")
//            throw SkipException()
//        }
//    }
}