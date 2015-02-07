package com.github.ykrasik.indexter.exception;

/**
 * @author Yevgeny Krasik
 */
@FunctionalInterface
public interface RunnableThrows {
    void run() throws Exception;
}
