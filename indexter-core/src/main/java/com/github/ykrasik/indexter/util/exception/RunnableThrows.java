package com.github.ykrasik.indexter.util.exception;

/**
 * @author Yevgeny Krasik
 */
@FunctionalInterface
public interface RunnableThrows {
    void run() throws Exception;
}
