package com.github.ykrasik.gamedex.persistence.exception;

/**
 * @author Yevgeny Krasik
 */
public class DataException extends RuntimeException {
    public DataException(String format, Object... args) {
        super(String.format(format, args));
    }

    public DataException(Throwable cause, String format, Object... args) {
        super(String.format(format, args), cause);
    }

    public DataException(Throwable cause) {
        super(cause);
    }
}
