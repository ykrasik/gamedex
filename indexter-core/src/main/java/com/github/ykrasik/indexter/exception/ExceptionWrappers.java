package com.github.ykrasik.indexter.exception;

import java.util.function.Consumer;

/**
 * @author Yevgeny Krasik
 */
public class ExceptionWrappers {
    public static <T> Consumer<T> rethrow(ConsumerWithException<T> c) {
        return elem -> {
            try {
                c.accept(elem);
            } catch (Exception ex) {
                /**
                 * within sneakyThrow() we cast to the parameterized type T.
                 * In this case that type is RuntimeException.
                 * At runtime, however, the generic types have been erased, so
                 * that there is no T type anymore to cast to, so the cast
                 * disappears.
                 */
                ExceptionWrappers.<RuntimeException>sneakyThrow(ex);
            }
        };
    }

    /**
     * Reinier Zwitserloot who, as far as I know, had the first mention of this
     * technique in 2009 on the java posse mailing list.
     * http://www.mail-archive.com/javaposse@googlegroups.com/msg05984.html
     */
    private static <T extends Throwable> T sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }
}
