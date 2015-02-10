package com.github.ykrasik.gamedex.common.exception;

/**
 * @author Yevgeny Krasik
 */
@FunctionalInterface
public interface SupplierThrows<T> {
    T get() throws Exception;
}
