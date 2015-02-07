package com.github.ykrasik.indexter.exception;

/**
 * @author Yevgeny Krasik
 */
@FunctionalInterface
public interface SupplierThrows<T> {
    T get() throws Exception;
}
