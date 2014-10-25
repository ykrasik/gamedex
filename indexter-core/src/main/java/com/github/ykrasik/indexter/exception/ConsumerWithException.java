package com.github.ykrasik.indexter.exception;

/**
 * @author Yevgeny Krasik
 */
@FunctionalInterface
public interface ConsumerWithException<T>{
    void accept(T elem) throws Exception;
}