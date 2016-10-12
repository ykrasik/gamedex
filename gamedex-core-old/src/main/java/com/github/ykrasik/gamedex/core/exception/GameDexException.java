package com.github.ykrasik.gamedex.core.exception;

/**
 * @author Yevgeny Krasik
 */
public class GameDexException extends RuntimeException {
    public GameDexException(String format, Object... args) {
        super(String.format(format, args));
    }

    public GameDexException(Throwable cause, String format, Object... args) {
        super(String.format(format, args), cause);
    }

    public GameDexException(Throwable cause) {
        super(cause);
    }
}
