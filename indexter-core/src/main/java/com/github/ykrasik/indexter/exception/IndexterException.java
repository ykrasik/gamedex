package com.github.ykrasik.indexter.exception;

/**
 * @author Yevgeny Krasik
 */
public class IndexterException extends RuntimeException {
    public IndexterException(String format, Object... args) {
        super(String.format(format, args));
    }

    public IndexterException(Throwable cause, String format, Object... args) {
        super(String.format(format, args), cause);
    }

    public IndexterException(Throwable cause) {
        super(cause);
    }
}
