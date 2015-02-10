package com.github.ykrasik.gamedex.common.exception;

/**
 * @author Yevgeny Krasik
 */
@FunctionalInterface
public interface FunctionThrows<T, R> {
    R apply(T t) throws Exception;
}
