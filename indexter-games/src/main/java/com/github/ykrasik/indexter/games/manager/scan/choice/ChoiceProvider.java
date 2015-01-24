package com.github.ykrasik.indexter.games.manager.scan.choice;

import com.github.ykrasik.indexter.games.datamodel.GamePlatform;
import com.github.ykrasik.indexter.games.info.GameRawBriefInfo;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * @author Yevgeny Krasik
 */
public interface ChoiceProvider {
    NoSearchResultsChoice getNoMetacriticSearchResultsChoice(Path path, String name, GamePlatform platform) throws Exception;

    MultipleSearchResultsChoice getMultipleMetacriticSearchResultsChoice(Path path,
                                                                         String name,
                                                                         GamePlatform platform,
                                                                         List<GameRawBriefInfo> briefInfos) throws Exception;

    MultipleSearchResultsChoice getMultipleGiantBombSearchResultsChoice(Path path,
                                                                        String name,
                                                                        GamePlatform platform,
                                                                        List<GameRawBriefInfo> briefInfos) throws Exception;

    Optional<GameRawBriefInfo> chooseFromMultipleResults(Path path,
                                                         String name,
                                                         GamePlatform platform,
                                                         List<GameRawBriefInfo> briefInfos) throws Exception;

    Optional<String> selectNewName(Path path, String name, GamePlatform platform) throws Exception;

    Optional<String> getLibraryName(Path path, String name, GamePlatform platform) throws Exception;

}
