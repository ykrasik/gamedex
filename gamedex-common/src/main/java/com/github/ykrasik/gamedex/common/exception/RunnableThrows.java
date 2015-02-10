package com.github.ykrasik.gamedex.common.exception;

/**
 * @author Yevgeny Krasik
 */
@FunctionalInterface
public interface RunnableThrows {
    void run() throws Exception;
}
