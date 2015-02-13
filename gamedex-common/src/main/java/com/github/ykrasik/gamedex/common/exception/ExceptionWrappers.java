package com.github.ykrasik.gamedex.common.exception;

import lombok.SneakyThrows;

import java.util.concurrent.Callable;

/**
 * @author Yevgeny Krasik
 */
public final class ExceptionWrappers {
    private ExceptionWrappers() { }

    @SneakyThrows
    public static <T> T rethrow(Callable<T> c) {
        return c.call();
    }

    @SneakyThrows
    public static void rethrow(RunnableThrows r) {
        r.run();
    }
}
