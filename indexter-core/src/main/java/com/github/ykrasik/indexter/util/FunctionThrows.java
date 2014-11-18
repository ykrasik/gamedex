package com.github.ykrasik.indexter.util;

/**
 * @author Yevgeny Krasik
 */
public interface FunctionThrows<T, R> {
    R apply(T t) throws Exception;
}
