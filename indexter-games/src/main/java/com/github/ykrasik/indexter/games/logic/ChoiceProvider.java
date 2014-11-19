package com.github.ykrasik.indexter.games.logic;

import com.github.ykrasik.indexter.games.datamodel.GamePlatform;
import com.github.ykrasik.indexter.games.info.GameRawBriefInfo;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * @author Yevgeny Krasik
 */
public interface ChoiceProvider {
    NoSearchResultsChoice getNoSearchResultsChoice(Path path, String name, GamePlatform platform) throws Exception;

    MultipleSearchResultsChoice getMultipleSearchResultsChoice(Path path,
                                                               String name,
                                                               GamePlatform platform,
                                                               List<GameRawBriefInfo> briefInfos) throws Exception;

    Optional<GameRawBriefInfo> chooseFromMultipleResults(Path path,
                                                         String name,
                                                         GamePlatform platform,
                                                         List<GameRawBriefInfo> briefInfos) throws Exception;

    Optional<String> selectNewName(Path path, String name, GamePlatform platform) throws Exception;

    Optional<String> getSubLibraryName(Path path, String name, GamePlatform platform) throws Exception;
}
