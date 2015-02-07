package com.github.ykrasik.indexter.exception;

/**
 * @author Yevgeny Krasik
 */
@FunctionalInterface
public interface ConsumerThrows<T>{
    void accept(T elem) throws Exception;
}