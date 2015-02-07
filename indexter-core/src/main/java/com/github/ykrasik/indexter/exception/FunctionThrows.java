package com.github.ykrasik.indexter.exception;

/**
 * @author Yevgeny Krasik
 */
@FunctionalInterface
public interface FunctionThrows<T, R> {
    R apply(T t) throws Exception;
}
