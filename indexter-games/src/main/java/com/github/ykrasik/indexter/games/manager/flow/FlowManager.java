package com.github.ykrasik.indexter.games.manager.flow;

import com.github.ykrasik.indexter.games.datamodel.persistence.Library;

import java.nio.file.Path;

/**
 * @author Yevgeny Krasik
 */
public interface FlowManager {
    void refreshLibraries(ExceptionHandler exceptionHandler);

    void cleanupGames(ExceptionHandler exceptionHandler);

    void processPath(Library library, Path path, ExceptionHandler exceptionHandler);
}
