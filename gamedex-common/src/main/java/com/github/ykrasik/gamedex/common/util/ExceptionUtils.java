package com.github.ykrasik.gamedex.common.util;

import lombok.SneakyThrows;

import java.util.concurrent.Callable;

/**
 * @author Yevgeny Krasik
 */
public final class ExceptionUtils {
    private ExceptionUtils() { }

    @SneakyThrows
    public static <T> T call(Callable<T> callable) {
        return callable.call();
    }
}
