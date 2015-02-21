package com.github.ykrasik.gamedex.provider.exception;

/**
 * @author Yevgeny Krasik
 */
public class GameInfoProviderException extends RuntimeException {
    public GameInfoProviderException(String message) {
        super(message);
    }

    public GameInfoProviderException(String format, Object... args) {
        this(String.format(format, args));
    }

    public GameInfoProviderException(String message, Throwable cause) {
        super(message, cause);
    }

    public GameInfoProviderException(Throwable cause) {
        super(cause);
    }
}
