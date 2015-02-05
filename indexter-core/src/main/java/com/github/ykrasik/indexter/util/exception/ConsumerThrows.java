package com.github.ykrasik.indexter.util.exception;

/**
 * @author Yevgeny Krasik
 */
@FunctionalInterface
public interface ConsumerThrows<T>{
    void accept(T elem) throws Exception;
}