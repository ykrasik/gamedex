package com.github.ykrasik.gamedex.common.exception;

/**
 * @author Yevgeny Krasik
 */
@FunctionalInterface
public interface ConsumerThrows<T>{
    void accept(T elem) throws Exception;
}