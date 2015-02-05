package com.github.ykrasik.indexter.games.manager.flow.choice;

import com.github.ykrasik.indexter.games.datamodel.GamePlatform;
import com.github.ykrasik.indexter.games.datamodel.info.giantbomb.GiantBombSearchResult;
import com.github.ykrasik.indexter.games.datamodel.info.metacritic.MetacriticSearchResult;
import com.github.ykrasik.indexter.games.manager.flow.choice.type.Choice;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * @author Yevgeny Krasik
 */
public interface ChoiceProvider {
    boolean shouldCreateLibrary(Path path) throws Exception;
    Optional<String> getLibraryName(Path path, GamePlatform platform) throws Exception;

    Choice onNoMetacriticSearchResults(String name, GamePlatform platform, Path path) throws Exception;
    Choice onNoGiantBombSearchResults(String name, GamePlatform platform, Path path) throws Exception;

    Choice onMultipleMetacriticSearchResults(String name, GamePlatform platform, Path path, List<MetacriticSearchResult> searchResults) throws Exception;
    Choice onMultipleGiantBombSearchResults(String name, GamePlatform platform, Path path, List<GiantBombSearchResult> searchResults) throws Exception;
}
