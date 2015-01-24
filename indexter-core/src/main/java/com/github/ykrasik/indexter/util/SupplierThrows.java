package com.github.ykrasik.indexter.util;

/**
 * @author Yevgeny Krasik
 */
public interface SupplierThrows<T> {
    T get() throws Exception;
}
