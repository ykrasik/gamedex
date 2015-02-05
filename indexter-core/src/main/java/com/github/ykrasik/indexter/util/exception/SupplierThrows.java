package com.github.ykrasik.indexter.util.exception;

/**
 * @author Yevgeny Krasik
 */
@FunctionalInterface
public interface SupplierThrows<T> {
    T get() throws Exception;
}
