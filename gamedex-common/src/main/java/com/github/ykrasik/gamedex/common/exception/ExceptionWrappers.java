package com.github.ykrasik.gamedex.common.exception;

import java.util.concurrent.Callable;

/**
 * @author Yevgeny Krasik
 */
public final class ExceptionWrappers {
    private ExceptionWrappers() { }

    public static <T> T rethrow(Callable<T> c) {
        try {
            return c.call();
        } catch (Exception ex) {
            throw ExceptionWrappers.<RuntimeException>sneakyThrow(ex);
        }
    }

    public static void rethrow(RunnableThrows r) {
        try {
            r.run();
        } catch (Exception e) {
            throw ExceptionWrappers.<RuntimeException>sneakyThrow(e);
        }
    }

    /**
     * Reinier Zwitserloot who, as far as I know, had the first mention of this
     * technique in 2009 on the java posse mailing list.
     * http://www.mail-archive.com/javaposse@googlegroups.com/msg05984.html
     */
    /**
     * within sneakyThrow() we cast to the parameterized type T.
     * In this case that type is RuntimeException.
     * At runtime, however, the generic types have been erased, so
     * that there is no T type anymore to cast to, so the cast
     * disappears.
     */
    private static <T extends Throwable> T sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }
}
