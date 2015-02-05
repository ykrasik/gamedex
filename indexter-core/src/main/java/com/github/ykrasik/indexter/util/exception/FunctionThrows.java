package com.github.ykrasik.indexter.util.exception;

/**
 * @author Yevgeny Krasik
 */
@FunctionalInterface
public interface FunctionThrows<T, R> {
    R apply(T t) throws Exception;
}
